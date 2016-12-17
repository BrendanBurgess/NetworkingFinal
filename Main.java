import java.util.*;
public class Main {

	private HashMap<Integer, HashSet<Edge>> adjacency;
	private HashMap<Integer, Integer> forwarding;
	private HashMap<Integer, Integer> seqNos;
	private HashMap<Integer, Integer> translate;
	private HashMap<Integer, Integer> translateReverse;
	private HashMap<Integer, Multicast> multicasts;
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

	private class Multicast{
		public int TTL;
		public HashSet<Integer> members;
		public Multicast(int ttl, HashSet<Integer> members){
			TTL = ttl;
			this.members = members;
		}
	}

	public Main(){
		adjacency = new HashMap<Integer, HashSet<Edge>>();
		forwarding = new HashMap<Integer, Integer>();
		seqNos = new HashMap< Integer, Integer>();
		translate = new HashMap< Integer, Integer>();
		translateReverse = new HashMap< Integer, Integer>();
		multicasts = new HashMap< Integer, Multicast>();
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
			if(values[1].equals("A")) createMulticast(values);
			if(values[1].equals("J")) joinMulticast(values);
			if(values[1].equals("Q")) quitMulticast(values);
			if(values[1].equals("F")){
				if (values.length == 3) forward(values);
				else 					forwardMulticast(values);
			}

		}
	}

	public void inital(String[] values){
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
	}

	// number nodes for easy access
	public int correctNodeValue(int node){
		if(translate.containsKey(node)){
			return translate.get(node);
		} 

		translate.put(node, nodeCount);
		translateReverse.put(nodeCount, node);
		node = nodeCount;
		nodeCount++;
		return node; 
	}

	public void createMulticast(String[] values){
		int source = correctNodeValue(Integer.parseInt(values[2]));
		int groupId = Integer.parseInt(values[3]);
		int ttl = Integer.parseInt(values[4]);
		HashSet<Integer> currentGroup = new HashSet<>();
		currentGroup.add(source);

		multicasts.put(groupId, new Multicast(ttl, currentGroup));

	}

	public void joinMulticast(String[] values){
		int multicast = Integer.parseInt(values[3]);
		int node = correctNodeValue(Integer.parseInt(values[2]));
		int time = Integer.parseInt(values[0]);
		if(multicasts.get(multicast).TTL < time) return;

		multicasts.get(multicast).members.add(node);
	}

	public void quitMulticast(String[] values){
		int multicast = Integer.parseInt(values[3]);
		int node = correctNodeValue(Integer.parseInt(values[2]));
		HashSet<Integer> nodes = multicasts.get(node).members;
		HashSet<Integer> newNodes = new HashSet<>();
		for(int n: nodes){
			if(n != node) newNodes.add(n);
		}
		multicasts.get(node).members = newNodes;
	}

	public void forwardMulticast(String[] values){
		System.out.println("hit the multis");
		int multicast = Integer.parseInt(values[3]);
		int node = correctNodeValue(Integer.parseInt(values[2]));
		int time = Integer.parseInt(values[0]);
		if(multicasts.get(multicast) ==  null) return;
		if(multicasts.get(multicast).TTL < time) return;

		HashMap<Integer, Integer> validNodes = onPath(node);
		for(int n: multicasts.get(multicast).members){
			if(validNodes.containsKey(n)) printForwardU(time, n, validNodes.get(n));
		}
	}

	public void linkState(String[] values){
		int currentNode = correctNodeValue(Integer.parseInt(values[2]));
		int currentSeqNo = Integer.parseInt(values[3]);
		if(seqNos.containsKey(currentNode)){
			if(seqNos.get(currentNode) >= currentSeqNo ){
				return;
			}
		}

		//Remove all\
		HashMap<Integer, HashSet<Edge>> newAdjacency = new HashMap<Integer, HashSet<Edge>>();
		for(int node: adjacency.keySet()){
			HashSet<Edge> adj = adjacency.get(node);
			HashSet<Edge> newAdj = new HashSet<Edge>();
			for(Edge e: adj){
				if(e.dest != currentNode) newAdj.add(e);
			}
			newAdjacency.put(node, newAdj);
		}
		adjacency = newAdjacency; 

		//Add New
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
		if(djikstrasNeeded) runDjikstras();

		int time_now = Integer.parseInt(values[0]);
		int destination = correctNodeValue(Integer.parseInt(values[2]));
		int nexthop = forwarding.get(destination);
		printForwardU(time_now,destination, nexthop);
		djikstrasNeeded = false;
	}


	private void runDjikstras(){

		int nodesNum = adjacency.size();
		Set<Integer> nodes = adjacency.keySet();
		HashMap<Integer, Integer> dist = new HashMap<>();
		HashMap<Integer, Integer> par = new HashMap<>();
		for(Integer node: nodes){
			dist.put(node, Integer.MAX_VALUE);
			par.put(node, -1);
		}
		dist.put(router, 0);
		IndexMinPQ<Integer> pq = new IndexMinPQ<>(nodesNum);
		pq.insert(router, 0);

		while(!pq.isEmpty()){
			int node = pq.delMin();
			HashSet<Edge> currentAdjacency = adjacency.get(node);
			for(Edge e: currentAdjacency){
				int curweight = e.weight;
				if(dist.get(e.dest) > dist.get(node) + e.weight){
					dist.put(e.dest, dist.get(node) + e.weight);
					par.put(e.dest, node);
					if(pq.contains(e.dest)) pq.changeKey(e.dest, dist.get(e.dest));
					else                    pq.insert(e.dest, dist.get(e.dest));
				}
			}
		}


		for(Integer node: nodes){
			if(node == router) continue;
			int  current = node;
			while(par.get(current) != router){
				current = par.get(current);
			}
			forwarding.put(node, current);
		}
	}

	private HashMap<Integer, Integer> onPath(int source){

		int nodesNum = adjacency.size();
		Set<Integer> nodes = adjacency.keySet();
		HashMap<Integer, Integer> dist = new HashMap<>();
		HashMap<Integer, Integer> par = new HashMap<>();
		for(Integer node: nodes){
			dist.put(node, Integer.MAX_VALUE);
			par.put(node, -1);
		}
		dist.put(source, 0);
		IndexMinPQ<Integer> pq = new IndexMinPQ<>(nodesNum);
		pq.insert(source, 0);

		while(!pq.isEmpty()){
			int node = pq.delMin();
			HashSet<Edge> currentAdjacency = adjacency.get(node);
			for(Edge e: currentAdjacency){
				int curweight = e.weight;
				if(dist.get(e.dest) > dist.get(node) + e.weight){
					dist.put(e.dest, dist.get(node) + e.weight);
					par.put(e.dest, node);
					if(pq.contains(e.dest)) pq.changeKey(e.dest, dist.get(e.dest));
					else                    pq.insert(e.dest, dist.get(e.dest));
				}
			}
		}

		System.out.println("Here?");
		HashMap<Integer, Integer> valids = new HashMap<>();
		for(Integer node: nodes){
			if(node == source) continue;
			int  current = node;
			int prev = -1;
			while(current != source && current != -1){
				if(current == router){ 
					valids.put(node, prev);
					break;
				}
				prev = current;
				current = par.get(current);
			}
		}
		System.out.println(valids.toString());
		return valids;
	}




	//This may be in reverse order ,check!
	private void printForwardU (int time_now, int destination, int nexthop){
		System.out.println("FU " + time_now + " " + translateReverse.get(destination) + " " + translateReverse.get(nexthop));
	}

	public static void main(String[] args){
		Main main = new Main();
		main.run();

	}
}