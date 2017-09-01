package gr.uoa.di.madgik.panagiotisl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Resources;

public class CoEuSReproducibleTest {

	private CoEuS coeus;
	
	@Before
	public void init() {
		coeus = new CoEuS();
		coeus.setInputEdgeList(Resources.getResource("amazon.txt").getFile());
		coeus.setInputEdgeListDelimiter(" ");
		coeus.setInputGroundTruthCommunities(Resources.getResource("amazonGTC.txt").getFile());
		coeus.setInputGroundTruthCommunitiesDelimiter(" ");
	}
	
	@Test
	public void simpleCoEuSTest() throws IOException {
		
		System.out.println("Testing: " + CoEuS.Increment.SIMPLE + ", " + CoEuS.SizeDetermination.GROUND_TRUTH);
		
		long startTime = System.nanoTime();
		coeus.setIncrement(CoEuS.Increment.SIMPLE);
		coeus.setSizeDetermination(CoEuS.SizeDetermination.GROUND_TRUTH);
		long endTime = System.nanoTime();
		long duration = (endTime - startTime);
		
		double f1score = coeus.execute();
		assertEquals(0.8, f1score, 0.01);
		System.out.println("F1-score: " + f1score);
		System.out.println("Milliseconds per community: " + duration / (1000000D * 963));
		
		
	}
	
	@Test
	public void edgeQualityVariationCoEuSTest() throws IOException {
		
		System.out.println("Testing: " + CoEuS.Increment.EDGE_QUALITY + ", " + CoEuS.SizeDetermination.GROUND_TRUTH);
		
		long startTime = System.nanoTime();
		coeus.setIncrement(CoEuS.Increment.EDGE_QUALITY);
		coeus.setSizeDetermination(CoEuS.SizeDetermination.GROUND_TRUTH);
		long endTime = System.nanoTime();
		long duration = (endTime - startTime);
		
		double f1score = coeus.execute();
		assertEquals(0.85, f1score, 0.01);
		System.out.println("F1-score: " + f1score);
		System.out.println("Milliseconds per community: " + duration / (1000000D * 963));
		
	}
	
	@Test
	public void edgeQualityVariationAndDropTailTest() throws IOException {

		System.out.println("Testing: " + CoEuS.Increment.EDGE_QUALITY + ", " + CoEuS.SizeDetermination.DROP_TAIL);
		long startTime = System.nanoTime();
		coeus.setIncrement(CoEuS.Increment.EDGE_QUALITY);
		coeus.setSizeDetermination(CoEuS.SizeDetermination.DROP_TAIL);
		long endTime = System.nanoTime();
		long duration = (endTime - startTime);
		
		double f1score = coeus.execute();
		assertEquals(0.8, f1score, 0.01);
		System.out.println("F1-score: " + f1score);
		System.out.println("Milliseconds per community: " + duration / (1000000D * 963));
		
	}
	
}
