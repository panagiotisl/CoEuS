package gr.uoa.di.madgik.panagiotisl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

public class CoEuS {

	public enum Increment {
		SIMPLE, EDGE_QUALITY
	};

	public enum SizeDetermination {
		GROUND_TRUTH, DROP_TAIL
	};

	private static final Logger LOGGER = Logger.getLogger(CoEuS.class);
	private static final int MAX_COMMUNITY_SIZE = 100;
	private static final Random rng = new Random(23);
	private static final int NUMBER_OF_SEEDS = 3;
	private static final int WINDOW_SIZE = 10000;
	private static final double EPS_OF_TOTAL_COUNT = 0.00001;
	private static final double CONFIDENCE = 0.99;

	private static double total = 0;
	private static int count = 0;

	private Increment increment;
	private SizeDetermination sizeDetermination;
	private String inputEdgeList;
	private String inputEdgeListDelimiter;
	private String inputGroundTruthCommunities;
	private String inputGroundTruthCommunitiesDelimiter;

	private static void getGroundTruthSizeDetermination(Community community, DoubleCountMinSketch commCMS, DoubleCountMinSketch degreeCMS) {
		
		int bestSize = Math.min(community.getGroundTruth().length, community.size());
		try {
			total += getF1Score(community.getPrunedCommunity(commCMS, degreeCMS, bestSize).keyset(), community.getGroundTruth());
			count += 1;
		} catch (Exception e) {
			LOGGER.error(e);
		}
		
	}

	private static void getDropTailSizeDetermination(Community community, DoubleCountMinSketch commCMS, DoubleCountMinSketch degreeCMS) {
		int bestSize = 0;
		List<Entry<String, Double>> sortedCommunity = community.getSortedCommunity();
		// calculate mean distance
		Double previous = null;
		double totalDifference = 0D;
		int totalDifferenceCount = 0;
		for (int i = NUMBER_OF_SEEDS; i < sortedCommunity.size(); i++) {
			Entry<String, Double> entry = sortedCommunity.get(i);
			if (previous != null && !community.isSeed(entry.getKey())) {
				totalDifference += previous - entry.getValue();
				totalDifferenceCount++;
			}
			previous = entry.getValue();
		}
		double meanDifference = totalDifference / totalDifferenceCount;

		List<Entry<String, Double>> reverseSortedCommunity = community.getSortedCommunity();
		Collections.reverse(reverseSortedCommunity);
		previous = null;
		int tailSize = 0;
		for (Entry<String, Double> entry : reverseSortedCommunity) {
			tailSize++;
			if (previous != null) {
				double difference = entry.getValue() - previous;
				if (difference > meanDifference) {
					break;
				}
			}
			previous = entry.getValue();
		}

		bestSize = community.size() - tailSize;
		try {
			total += getF1Score(community.getPrunedCommunity(commCMS, degreeCMS, bestSize).keyset(), community.getGroundTruth());
			count += 1;
		} catch (Exception e) {
			LOGGER.error(e);
		}
	}

	private static Set<Integer> getRandomNumbers(int max, int numbersNeeded) {
		if (max < numbersNeeded) {
			throw new IllegalArgumentException("Can't ask for more numbers than are available");
		}
		// Note: use LinkedHashSet to maintain insertion order
		Set<Integer> generated = new LinkedHashSet<Integer>();
		while (generated.size() < numbersNeeded) {
			Integer next = rng.nextInt(max);
			// As we're adding to a set, this will automatically do a containment check
			generated.add(next);
		}
		return generated;
	}

	private static void addToCommCMS(String[] nodes,
			DoubleCountMinSketch commCMS, DoubleCountMinSketch degreeCMS,
			List<Community> communities) {

		for (int i = 0; i < communities.size(); i++) {
			Community comm = communities.get(i);
			// if adjacent node is a seed, add 1
			if (comm.isSeed(nodes[0])) {
				commCMS.add(i + ":" + nodes[1], 1);
			}
			// else if adjacent node is a member add estimate of participation /
			// estimate of degree
			else if (comm.contains(nodes[0])) {
				commCMS.add(i + ":" + nodes[1], 1);
			}
			// if adjacent node is a seed, add 1
			if (comm.isSeed(nodes[1])) {
				commCMS.add(i + ":" + nodes[0], 1);
			}
			// else if adjacent node is a member add estimate of participation /
			// estimate of degree
			else if (comm.contains(nodes[1])) {
				commCMS.add(i + ":" + nodes[0], 1);
			}
			// if adjacent node is a member add node to community
			if (comm.contains(nodes[0])) {
				comm.put(nodes[1], 1 - ((degreeCMS.estimateCount(nodes[1]) - commCMS.estimateCount(i + ":" + nodes[1])) / degreeCMS.estimateCount(nodes[1])));
			}
			if (comm.contains(nodes[1])) {
				comm.put(nodes[0], 1 - ((degreeCMS.estimateCount(nodes[0]) - commCMS.estimateCount(i + ":" + nodes[0])) / degreeCMS.estimateCount(nodes[0])));
			}
		}
	}

	private static void addToCommCMSPRV(String[] nodes,
			DoubleCountMinSketch commCMS, DoubleCountMinSketch degreeCMS, List<Community> communities) {

		for (int i = 0; i < communities.size(); i++) {
			Community comm = communities.get(i);
			// if adjacent node is a seed, add 1
			if (comm.isSeed(nodes[0])) {
				commCMS.add(i + ":" + nodes[1], 1);
			}
			// else if adjacent node is a member add estimate of participation /
			// estimate of degree
			else if (comm.contains(nodes[0])) {
				commCMS.add(i + ":" + nodes[1], comm.get(nodes[0]));
			}
			// if adjacent node is a seed, add 1
			if (comm.isSeed(nodes[1])) {
				commCMS.add(i + ":" + nodes[0], 1);
			}
			// else if adjacent node is a member add estimate of participation /
			// estimate of degree
			else if (comm.contains(nodes[1])) {
				commCMS.add(i + ":" + nodes[0], comm.get(nodes[1]));
			}
			// if adjacent node is a member add node to community
			if (comm.contains(nodes[0])) {
				comm.put(nodes[1], 1 - ((degreeCMS.estimateCount(nodes[1]) - commCMS.estimateCount(i + ":" + nodes[1])) / degreeCMS.estimateCount(nodes[1])));
			}
			if (comm.contains(nodes[1])) {
				comm.put(nodes[0], 1 - ((degreeCMS.estimateCount(nodes[0]) - commCMS.estimateCount(i + ":" + nodes[0])) / degreeCMS.estimateCount(nodes[0])));
			}
		}
	}

	private static void addToDegreeCMS(String[] nodes, DoubleCountMinSketch degreeCMS) {
		degreeCMS.add(nodes[0], 1);
		degreeCMS.add(nodes[1], 1);
	}

	private static double getPrecision(Set<String> found, Set<String> gtc,
			SetView<String> common) {
		return (double) common.size() / found.size();
	}

	private static double getRecall(Set<String> found, Set<String> gtc,
			SetView<String> common) {
		return (double) common.size() / gtc.size();
	}

	private static double getF1Score(Set<String> found, String[] comm) {
		HashSet<String> gtc = new HashSet<String>(Arrays.asList(comm));
		SetView<String> common = Sets.intersection(found, gtc);
		double precision = getPrecision(found, gtc, common);
		double recall = getRecall(found, gtc, common);
		if (precision == 0 && recall == 0)
			return 0;
		else
			return 2 * (precision * recall) / (precision + recall);
	}

	public void setIncrement(Increment increment) {
		this.increment = increment;
	}

	public void setSizeDetermination(SizeDetermination sizeDetermination) {
		this.sizeDetermination = sizeDetermination;
	}

	public void setInputEdgeList(String inputEdgeList) {
		this.inputEdgeList = inputEdgeList;

	}

	public void setInputEdgeListDelimiter(String inputEdgeListDelimiter) {
		this.inputEdgeListDelimiter = inputEdgeListDelimiter;

	}

	public void setInputGroundTruthCommunities(
			String inputGroundTruthCommunities) {
		this.inputGroundTruthCommunities = inputGroundTruthCommunities;
	}

	public void setInputGroundTruthCommunitiesDelimiter(
			String inputGroundTruthCommunitiesDelimiter) {
		this.inputGroundTruthCommunitiesDelimiter = inputGroundTruthCommunitiesDelimiter;

	}

	public Double execute() throws IOException {
		
		total = 0;
		count = 0;
		
		BufferedReader gtcFileBR = new BufferedReader(new FileReader(new File(
				this.inputGroundTruthCommunities)));

		String readLine;

		List<Community> communities = new ArrayList<Community>();
		String commLine;

		while ((commLine = gtcFileBR.readLine()) != null) {
			String[] comm = commLine.trim().split(
					this.inputGroundTruthCommunitiesDelimiter);
			Set<Integer> randomNumbers = getRandomNumbers(comm.length,
					NUMBER_OF_SEEDS);

			HashSet<String> set = new HashSet<String>();
			for (int number : randomNumbers) {
				set.add(comm[number]);
			}
			communities.add(new Community(set, comm));
		}
		File graphFile = new File(this.inputEdgeList);
		BufferedReader graphFileBR = new BufferedReader(new FileReader(
				graphFile));

		int elementsProcessed = 0;
		DoubleCountMinSketch commCMS = new DoubleCountMinSketch(
				EPS_OF_TOTAL_COUNT, CONFIDENCE, 23);
		DoubleCountMinSketch degreeCMS = new DoubleCountMinSketch(
				EPS_OF_TOTAL_COUNT, CONFIDENCE, 23);

		while ((readLine = graphFileBR.readLine()) != null) {

			elementsProcessed++;
			String nodes[] = readLine.split(this.inputEdgeListDelimiter);
			// do not allow self loops
			if (nodes[0].equals(nodes[1]))
				continue;
			addToDegreeCMS(nodes, degreeCMS);
			switch (this.increment) {
			case SIMPLE:
				addToCommCMS(nodes, commCMS, degreeCMS, communities);
				break;
			case EDGE_QUALITY:
			default:
				addToCommCMSPRV(nodes, commCMS, degreeCMS, communities);
				break;
			}
			if (elementsProcessed % WINDOW_SIZE == 0) {
				communities.parallelStream().forEach(
						(community) -> {
							community.pruneCommunity(commCMS, degreeCMS,
									MAX_COMMUNITY_SIZE);
						});
			}
		}
		graphFileBR.close();
		communities.forEach((community) -> {
			switch (this.sizeDetermination) {
			case GROUND_TRUTH:
				getGroundTruthSizeDetermination(community, commCMS, degreeCMS);
				break;
			case DROP_TAIL:
			default:
				getDropTailSizeDetermination(community, commCMS, degreeCMS);
				break;

			}
			});
		gtcFileBR.close();
		return total / count ;
	}

}
