package ca.mcgill.science.tepid.client.lpd;

import ca.mcgill.science.tepid.models.data.PrintJob;

import java.io.InputStream;

public interface LPDJobListener {
    void printJob(PrintJob p, InputStream is);
}
