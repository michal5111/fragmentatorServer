package com.michal5111.fragmentator_server.enums;

public enum DownloadStatusType {
    DOWNLOAD("[download]"), INFO("[info]"), YOUTUBE("[youtube]"), WARNING("WARNING:"), FFMPEG("[ffmpeg]"), OTHER("");

    public final String value;

    DownloadStatusType(String s) {
        this.value = s;
    }
}
