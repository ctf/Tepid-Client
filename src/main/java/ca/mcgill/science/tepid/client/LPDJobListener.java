package ca.mcgill.science.tepid.client;

import ca.mcgill.science.tepid.common.PrintJob;

import java.io.InputStream;

public interface LPDJobListener {
	void printJob(PrintJob p, InputStream is);
}
