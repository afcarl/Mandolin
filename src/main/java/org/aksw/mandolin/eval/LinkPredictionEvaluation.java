package org.aksw.mandolin.eval;

import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class LinkPredictionEvaluation {

	public static void main(String[] args) throws IOException {
		
		// benchmark/wn18/wordnet-mlj12-test.nt eval/wn18_09_1m_v
		
		final String REF = "benchmark/wn18/wordnet-mlj12-test.nt";
		final String BASE = "eval/wn18_01_???m_va";
		
		ArrayList<Double> meanranks = new ArrayList<>();
		
		for(int i=1; i<=1; i++) {
			
			// dirty hack
			if(i==6) i = 10;
			
			String testSet = REF;
			String output = BASE.replace("???", String.valueOf(i));
			
			HitsAtK hk = new HitsAtK(testSet, output);
			hk.setMinThr(0);
			hk.start();
		
			MeanRankCalc mr = new MeanRankCalc(testSet, output);
			mr.setMinThr(0);
			mr.partitionData();
			meanranks.add(mr.start());
			
		}
		
		System.out.println("\nmeanranks = " + meanranks);

	}

}