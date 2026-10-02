use bytes::Bytes;
use std::io::{self, Read};
use tokio::sync::mpsc;
use tokio_stream::wrappers::ReceiverStream;
use tokio_util::{
    codec::{FramedRead, LinesCodec},
    io::StreamReader,
};

const MAX_FRAME_BYTES: usize = 1024 * 1024;
type Input = FramedRead<StreamReader<ReceiverStream<io::Result<Bytes>>, Bytes>, LinesCodec>;

/// Only the blocking stdin bridge is thread-based. LinesCodec owns framing, UTF-8 and size limits.
/// A detached reader lets the process exit even when the parent keeps stdin open; Tokio's stdin
/// otherwise keeps a non-cancellable blocking read alive during runtime shutdown.
pub(crate) fn input_channel() -> Input {
    let (tx, rx) = mpsc::channel(4);
    std::thread::spawn(move || {
        let stdin = io::stdin();
        let mut input = stdin.lock();
        let mut buffer = [0u8; 8192];
        loop {
            match input.read(&mut buffer) {
                Ok(0) => break,
                Ok(count) => {
                    if tx
                        .blocking_send(Ok(Bytes::copy_from_slice(&buffer[..count])))
                        .is_err()
                    {
                        break;
                    }
                }
                Err(error) if error.kind() == io::ErrorKind::Interrupted => continue,
                Err(error) => {
                    let _ = tx.blocking_send(Err(error));
                    break;
                }
            }
        }
    });
    FramedRead::new(
        StreamReader::new(ReceiverStream::new(rx)),
        LinesCodec::new_with_max_length(MAX_FRAME_BYTES),
    )
}
