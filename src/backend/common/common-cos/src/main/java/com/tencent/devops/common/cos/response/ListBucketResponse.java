package com.tencent.devops.common.cos.response;

import com.tencent.devops.common.cos.model.exception.COSException;
import com.tencent.devops.common.cos.model.pojo.ListBucketResult;
import okhttp3.Response;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;

/**
 * Created by schellingma on 2017/04/27.
 * Powered By Tencent
 */
public class ListBucketResponse extends BaseResponse {

    private ListBucketResult listBucketResult;

    @Override
    public void parseResponse(Response response) throws COSException {
        if (response.isSuccessful()) {
            try {
                setSuccess(true);
                parseContent(response.body().bytes());

            } catch (IOException e) {
                setSuccess(false);
                setErrorMessage(e.getMessage());
            }
        } else {
            setSuccess(false);
            setCommonErrorMessage(response.code());
        }
    }

    private void parseContent(byte[] content) throws IOException {
        if(content == null || content.length == 0)
            return;

        try {
            JAXBContext context = JAXBContext.newInstance(ListBucketResult.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            listBucketResult = (ListBucketResult) unmarshaller.unmarshal(new ByteArrayInputStream(content));
        } catch (JAXBException e) {
            throw new IOException("Parse ListBucketResult XML content failed", e);
        }
    }

    public ListBucketResult getListBucketResult() {
        return listBucketResult;
    }



}
