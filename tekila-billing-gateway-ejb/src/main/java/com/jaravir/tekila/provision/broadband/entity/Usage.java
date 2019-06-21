package com.jaravir.tekila.provision.broadband.entity;

/**
 * Created by khsadigov on 2/27/2017.
 */
public class Usage {

    private double download;
    private double upload;
    private String downloadTxt;
    private String uploadTxt;

    public Usage(double download, double upload, String downloadTxt, String uploadTxt) {
        this.download = download;
        this.upload = upload;
        this.downloadTxt = downloadTxt;
        this.uploadTxt = uploadTxt;
    }

    public double getDownload() {
        return download;
    }

    public double getUpload() {
        return upload;
    }

    public String getDownloadString() {
        return Double.toString(download) + " (" + downloadTxt + ")";
    }

    public String getUploadString() {
        return Double.toString(upload) + " (" + uploadTxt + ")";
    }
}
