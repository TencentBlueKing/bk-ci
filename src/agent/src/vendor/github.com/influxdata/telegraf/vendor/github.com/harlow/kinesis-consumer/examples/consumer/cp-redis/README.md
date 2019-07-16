# Consumer

Read records from the Kinesis stream

### Environment Variables

Export the required environment vars for connecting to the Kinesis stream and Redis for checkpoint:

```
export AWS_ACCESS_KEY=
export AWS_REGION=
export AWS_SECRET_KEY=
export REDIS_URL=
```

### Run the consumer

    $ go run main.go --app appName --stream streamName
