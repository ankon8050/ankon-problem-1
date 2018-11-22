package com.ankon.problem1.domain;

public class Result {

    String SHA;
    String filename;
    String oldSignature;
    String newSignature;

    public Result(String SHA, String filename, String oldSignature, String newSignature) {
        this.SHA = SHA;
        this.filename = filename;
        this.oldSignature = oldSignature;
        this.newSignature = newSignature;
    }

    public String getSHA() {
        return SHA;
    }

    public void setSHA(String SHA) {
        this.SHA = SHA;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getOldSignature() {
        return oldSignature;
    }

    public void setOldSignature(String oldSignature) {
        this.oldSignature = oldSignature;
    }

    public String getNewSignature() {
        return newSignature;
    }

    public void setNewSignature(String newSignature) {
        this.newSignature = newSignature;
    }

    @Override
    public String toString() {
        return "Result{" +
                "SHA='" + SHA + '\'' +
                ", filename='" + filename + '\'' +
                ", oldSignature='" + oldSignature + '\'' +
                ", newSignature='" + newSignature + '\'' +
                '}';
    }
}
