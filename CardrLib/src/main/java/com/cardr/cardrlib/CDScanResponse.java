package com.cardr.cardrlib;

public class CDScanResponse {
    public String dtcCode = "";
    public String dtcDesc  = "";
    public String dtcStatus = "";

    public String getDtcCode() {
        return dtcCode;
    }

    public void setDtcCode(String dtcCode) {
        this.dtcCode = dtcCode;
    }

    public String getDtcDesc() {
        return dtcDesc;
    }

    public void setDtcDesc(String dtcDesc) {
        this.dtcDesc = dtcDesc;
    }

    public String getDtcStatus() {
        return dtcStatus;
    }

    public void setDtcStatus(String dtcStatus) {
        this.dtcStatus = dtcStatus;
    }
}
