package com.myfiziq.sdk.db;

import android.app.DownloadManager;

import java.util.LinkedList;
import java.util.List;

public class ResourceDownloadStatus
{
    public List<Resource> resources = new LinkedList<>();

    public List<Resource> getResources()
    {
        return resources;
    }

    public void addResource(Resource resource)
    {
        resources.add(resource);
    }

    public void addResource(String filename, int status, int reason, long downloadedBytes, long remainingBytes)
    {
        Resource resource = new Resource(filename, status, reason, downloadedBytes, remainingBytes);
        resources.add(resource);
    }

    public long getDownloadedBytes()
    {
        long downloadedBytes = 0;

        for (Resource resource : resources)
        {
            downloadedBytes += resource.getDownloadedBytes();
        }

        return downloadedBytes;
    }

    public long getTotalBytes()
    {
        long totalBytes = 0;

        for (Resource resource : resources)
        {
            totalBytes += resource.getTotalBytes();
        }

        return (int) totalBytes;
    }

    public boolean isAllSuccessful()
    {
        for (Resource resource : resources)
        {
            if (resource.getStatus() != DownloadManager.STATUS_SUCCESSFUL)
            {
                return false;
            }
        }

        return true;
    }

    public boolean hasAnyFailed()
    {
        for (Resource resource : resources)
        {
            if (resource.getStatus() == DownloadManager.STATUS_FAILED
                    || resource.getReason() == DownloadManager.PAUSED_WAITING_FOR_NETWORK
                    || resource.getReason() == DownloadManager.PAUSED_WAITING_TO_RETRY)
            {
                return true;
            }
        }

        return false;
    }

    public static class Resource
    {
        private String filename;
        private int status;
        private int reason;
        private long downloadedBytes;
        private long totalBytes;

        public Resource(String filename, int status, int reason, long downloadedBytes, long totalBytes)
        {
            this.filename = filename;
            this.status = status;
            this.reason = reason;
            this.downloadedBytes = downloadedBytes;
            this.totalBytes = totalBytes;
        }

        public String getFilename()
        {
            return filename;
        }

        public int getStatus()
        {
            return status;
        }

        public int getReason()
        {
            return reason;
        }

        public long getDownloadedBytes()
        {
            return downloadedBytes;
        }

        public long getTotalBytes()
        {
            return totalBytes;
        }

        public boolean isComplete()
        {
            return downloadedBytes == totalBytes;
        }
    }
}
