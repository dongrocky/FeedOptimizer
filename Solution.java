import java.util.ArrayList;
import java.util.Arrays;
import java.io.*;
import java.util.Stack;
public class Solution {
	
	public void solution(ArrayList<Event> events, int N, int W, int H) {
		/* This solution uses dynamic programing to solve the issue. Say val[i][h] 
		 	denotes the max score one can get from the first story within time window 
		 	to the i th story under limit height of h. The transfrom function can be 
		 	written as :
		 	val[i][h] = max{val[i-1][h], val[i-1][h-hi] + si}
		 	hi is the height of the ith story and si is the score of ith story.
		 	Then val[N][H] will the final highest score one can get.
		 */
		int winLeft = 0;
		//boolean winLeftChanged = true;
		int lastWinRight = -1;
		int val[] = new int[H+1];
		int pick[][] = new int[N+1][H+1];
		int pickStoryNum[] = new int[H+1];
		int totalEvents = events.size();
		int sid = 0;
		int hRemaining = H;
		StoryEvent se = null;
		Stack<Integer> storyPicked = new Stack<Integer>();
		StoryEvent minScoreStory = null;
		StoryEvent maxHeightStory = null;
		int solutionHeight = 0;
		
		Arrays.fill(val, 0);
		Arrays.fill(pickStoryNum, 0);
		for(int j=0; j<N+1; j++) {
			Arrays.fill(pick[j], -1);
		}
		
		for(int i=0; i<totalEvents; i++) {
			Event e = events.get(i);
			if(e.getType().equals("R")) {
				// It's reload event, first we get the left side of the window
				for(;winLeft < i; winLeft++) {
					Event leftEvent = events.get(winLeft);
					if(leftEvent.getType().equals("S")) {
						if(e.getTime() - leftEvent.getTime() <= W) {
							break;
						}					
					}
				}
				
				// calc the score from this left event
				sid = calcScore(lastWinRight+1, i, val, pick, pickStoryNum, events, sid, minScoreStory, maxHeightStory, solutionHeight);
				hRemaining = H;
				se = null;
				solutionHeight = 0;
				// get the stories picked.
				for(int s=sid; s>0 && hRemaining > 0; s--) {
					if(pick[s][hRemaining] != -1) {
						se = (StoryEvent) events.get(pick[s][hRemaining]);
						storyPicked.push(se.getSid());
						hRemaining -= se.getHeight();
						if(minScoreStory == null || se.getScore() < minScoreStory.getScore()) {
							minScoreStory = se;
						}
						if(maxHeightStory == null || se.getHeight() > maxHeightStory.getHeight()) {
							maxHeightStory = se;
						}
						solutionHeight += se.getHeight();
					}
				}
				if (events.get(winLeft).getType().equals("S")){
					StoryEvent leftEvent = (StoryEvent) events.get(winLeft);
					if(se != null && se.getSid() < leftEvent.getSid()) {
						
						Arrays.fill(val, 0);
						Arrays.fill(pickStoryNum, 0);
						for(int j=0; j<N+1; j++) {
							Arrays.fill(pick[j], -1);
						}
						storyPicked.clear();
						minScoreStory = null;
						maxHeightStory = null;
						solutionHeight = 0;
						sid = calcScore(winLeft, i, val, pick, pickStoryNum, events, 0, minScoreStory, maxHeightStory, solutionHeight);

						// get the stories picked.
						hRemaining = H;
						se = null;
						for(int s=sid; s>0; s--) {
							if(pick[s][hRemaining] != -1) {
								se = (StoryEvent) events.get(pick[s][hRemaining]);
								storyPicked.push(se.getSid());
								hRemaining -= se.getHeight();
								if(minScoreStory == null || se.getScore() < minScoreStory.getScore()) {
									minScoreStory = se;
								}
								if(maxHeightStory == null || se.getHeight() > maxHeightStory.getHeight()) {
									maxHeightStory = se;
								}
								solutionHeight += se.getHeight();
							}
						}
					}
				}
				
				System.out.print(val[H] + " " + storyPicked.size());
				while(storyPicked.size() > 0) {
					System.out.print(" " + storyPicked.pop());
				}
				System.out.print("\n");
				
				// prepare data for next round				
				lastWinRight = i;
			}
		}
	}
	
	private int calcScore(int start, int end, int[] val, int[][] pick, 
			int[] pickStoryNum, ArrayList<Event> events, int sid, StoryEvent minScoreStory, 
			StoryEvent maxHeightStory, int solutionHeight) {
		for(int j=start; j<=end; j++) {
			int H = val.length-1;
			Event curEvent = events.get(j);
			if(curEvent.getType().equals("S")) {
				StoryEvent curStory = (StoryEvent) curEvent;
				int curScore = curStory.getScore();
				int curHeight = curStory.getHeight();
				sid++;
				if(((minScoreStory != null && curScore <= minScoreStory.getScore() 
						&& curHeight >= minScoreStory.getHeight()) || 
						(maxHeightStory != null && curHeight >= maxHeightStory.getHeight() && 
						curScore <= maxHeightStory.getScore()))  && curHeight + solutionHeight > H) {
					continue;
				}
				for(int h=H; h>=curHeight; h--) {
					int subHeight = h - curHeight;
					if(val[h] < val[subHeight] + curScore){
						// picking current story
						val[h] = val[subHeight] + curScore;
						pick[sid][h] = j;
						pickStoryNum[h] = pickStoryNum[subHeight] + 1;
					} else if (val[h] == val[subHeight] + curScore){
						// tie situation
						if(pickStoryNum[h] > pickStoryNum[subHeight] + 1) {
							// picking current story since there are fewer stories 
							val[h] = val[subHeight] + curScore;
							pick[sid][h] = j;
							pickStoryNum[h] = pickStoryNum[subHeight] + 1;
						} 
					}

				}
			}
		}
		
		return sid;
	}
	
	public static void main(String args[]) throws Exception {
		// read in data
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		ArrayList<Event> events = new ArrayList<Event>();
		Solution fo = new Solution();
		
		String line = br.readLine();
		String[] nums = line.split(" ");
		int count = 0, numStory = 0;
		if(nums == null || nums.length != 3) {
			throw new Exception("input format wrong.");
		}
		int N = Integer.parseInt(nums[0]);
		int W = Integer.parseInt(nums[1]);
		int H = Integer.parseInt(nums[2]);
		
		if(N == 0 || W == 0 || H == 0) return;
		
		while((line = br.readLine()) != null && count < N) {
			String[] event = line.split(" ");
			if (event != null && event.length > 0) {
				if(event[0].equals("S") && event.length >= 4) {
					events.add(new StoryEvent("S", Integer.parseInt(event[1]),
							Integer.parseInt(event[2]), Integer.parseInt(event[3]), 
							++numStory));
					
				} else if(event[0].equals("R") && event.length >= 2) {
					events.add(new ReloadEvent("R", Integer.parseInt(event[1])));
				}
			}
			count++;
		}
		
		// solve the issue;
		fo.solution(events, numStory, W, H);
	}
}

class Event {
	private String type;
	private int time;
    public Event(String t, int time) {
    	type = t;
    	this.time = time;
    }
    
    public String getType() {
    	return type;
    }
    
	public int getTime() {
		return time;
	}
}

class ReloadEvent extends Event {
	public ReloadEvent(String t, int time) {
		super(t, time);
	}
	

}

class StoryEvent extends Event {
	private int score;
	private int  height;
	private int sid;
	public StoryEvent(String t, int time, int score, int height, int sid) {
		super(t, time);
		this.score = score;
		this.height = height;
		this.sid = sid;
	}
	
	public int getScore() {
		return score;
	}
	
	public int getHeight() {
		return height;
	}
	
	public int getSid() {
		return sid;
	}
}