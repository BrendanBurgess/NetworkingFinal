import java.util.*;
public class Main {

	private HashMap<Integer, HashSet<Edge>> adjacency;
	private HashMap<Integer, Integer> forwarding;
	private HashMap<Integer, Integer> seqNos;
	private HashMap<Integer, Integer> translate;
	public int nodeCount = 0;
	public int router = 0;
	public boolean djikstrasNeeded = true; 

	private class Edge{
		public int weight;
		public int dest;
		public Edge(int weight, int dest){
			this.weight = weight;
			this.dest = dest;
		}
	}

	public Main(){
		adjacency = new HashMap<Integer, HashSet<Edge>>();
		forwarding = new HashMap<Integer, Integer>();
		seqNos = new HashMap< Integer, Integer>();
		translate = new HashMap< Integer, Integer>();
	}
	
	public void run(){
		Scanner sc = new Scanner(System.in);
		while(sc.hasNext()){
			String line = sc.nextLine();
			line = line.replace("<", "");
			line = line.replace(">", "");
			line = line.replace(",", "");
			String[] values = line.split("\\s+");
			if(values[0].equals("#") || values.length < 2) continue; // for comments, in a comment
			
			if(values[1].equals("I")) inital(values);
			if(values[1].equals("L")) linkState(values);
			if(values[1].equals("F")) forward(values);

		}
	}

	public void inital(String[] values){
		System.out.println("reached initalizer");
		router = correctNodeValue(Integer.parseInt(values[2]));
		HashSet<Edge> currentAdjacency = new HashSet<>();
		for(int i = 3; i < values.length; i+=2){
			int endNode = correctNodeValue(Integer.parseInt(values[i]));
			currentAdjacency.add(new Edge(Integer.parseInt(values[i+ 1]), endNode));
			HashSet<Edge> endNodeAdjacency = new HashSet<>();
			endNodeAdjacency.add(new Edge(Integer.parseInt(values[i+ 1]), router));
			adjacency.put(endNode, endNodeAdjacency);
		}
		adjacency.put(router, currentAdjacency);
		for(Edge e: currentAdjacency){
			System.out.println("node = " + " is adjacent to " + e.dest);
		}
	}

	// number nodes for easy access
	public int correctNodeValue(int node){
		if(translate.containsKey(node)){
			return translate.get(node);
		} 

		translate.put(node, nodeCount);
		node = nodeCount;
		nodeCount++;
		return node; 
	}

	public void linkState(String[] values){
		System.out.println("reached Link State");
		int currentNode = correctNodeValue(Integer.parseInt(values[2]));
		int currentSeqNo = Integer.parseInt(values[3]);
		if(seqNos.containsKey(currentNode)){
			if(seqNos.get(currentNode) >= currentSeqNo ){
				return;
			}
		}

		HashSet<Edge> currentAdjacency = new HashSet<>();
		for(int i = 4; i < values.length; i+=2){
			int endNode = correctNodeValue(Integer.parseInt(values[i]));
			currentAdjacency.add(new Edge(Integer.parseInt(values[i+ 1]),
										  endNode));

			//need to check for sequence order?
			HashSet<Edge> otherAdjaceny = null;
			if(adjacency.containsKey(endNode)){
				otherAdjaceny = adjacency.get(endNode);
				HashSet<Edge> newAdjaceny = new HashSet<>();
				for(Edge e: otherAdjaceny){
					if(e.dest != currentNode) newAdjaceny.add(e);
				}
				newAdjaceny.add(new Edge(Integer.parseInt(values[i+ 1]), currentNode ));
				otherAdjaceny = newAdjaceny;
			} else {
				otherAdjaceny = new HashSet<Edge>();
				otherAdjaceny.add(new Edge(Integer.parseInt(values[i+ 1]), currentNode));
			}
			adjacency.put(endNode, otherAdjaceny);
		}

		djikstrasNeeded = true;
		adjacency.put(currentNode, currentAdjacency);
		seqNos.put(currentNode, currentSeqNo);

	}

	public void forward(String[] values){
		System.out.println("reached forward?");
		if(djikstrasNeeded) runDjikstras();

		//int time_now = Integer.parseInt(values[0]);
		//int destination = Integer.parseInt(values[2]);
		//int nexthop = forwarding.get(destination);
		//printForwardU(time_now,destination, nexthop);
		djikstrasNeeded = false;
	}

	//need to update for how min pq works, shit
	private void runDjikstras(){
		System.out.println("adjacency of 5 = " + adjacency.get(5));

		int nodesNum = adjacency.size();
		Set<Integer> nodes = adjacency.keySet();
		HashMap<Integer, Integer> dist = new HashMap<>();
		HashMap<Integer, Integer> par = new HashMap<>();
		for(Integer node: nodes){
			dist.put(node, Integer.MAX_VALUE);
			par.put(node, -1);
		}
		System.out.println(dist.toString());
		dist.put(router, 0);
		IndexMinPQ<Integer> pq = new IndexMinPQ<>(nodesNum);
		pq.insert(router, 0);

		int counter = 10;
		while(!pq.isEmpty() && counter > 0){
			int node = pq.delMin();
			System.out.println("node = " + node);
			System.out.println("adjacency = " + adjacency.get(node));
			HashSet<Edge> currentAdjacency = adjacency.get(node);
			for(Edge e: currentAdjacency){
				System.out.println("node = " + " is adjacent to " + e.dest);
				int curweight = e.weight;
				if(dist.get(e.dest) > dist.get(node) + e.weight){
					dist.put(e.dest, dist.get(node) + e.weight);
					par.put(e.dest, node);
				}
				if(pq.contains(e.dest)) pq.changeKey(e.dest, dist.get(e.dest));
				else                    pq.insert(e.dest, dist.get(e.dest));
				//System.out.println(dist.toString());
				//System.out.println("adjacency of 4 = " + adjacency.get(4));
			}
			counter --;
		}
	}

	//This may be in reverse order ,check!
	private void printForwardU (int time_now, int destination, int nexthop){
		System.out.println("FU " + time_now + " " + destination + " " + nexthop);
	}

	public static void main(String[] args){
		Main main = new Main();
		main.run();

	}
}