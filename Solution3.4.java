/* pass 4 cases */

import java.util.ArrayList;
import java.util.Collections;
import java.io.*;


public class FeedOptimizer {
	static boolean debug = false;
	private SolutionInfo best;
	private SolutionInfo curSolution;
	private ArrayList<Integer> sids;
	public FeedOptimizer(int N) {
		best = new SolutionInfo(N);
		curSolution = new SolutionInfo(N);
		sids = new ArrayList<Integer>();
	}
	public void solution(ArrayList<Event> events, int N, int W, int H) {
		ArrayList<StoryEvent> storyEvents = new ArrayList<StoryEvent>();
		boolean recompute = true;
		Event event = null;
		StoryEvent story = null;
		float ratio = 0;
		int storyCount = 0;
		int removeNum = 0;
		int numStory = 0;

		N = Math.min(N, events.size());
		
		// check whether its a story or reload
		for(int i=0; i<N; i++) {
			event = events.get(i);
			if(event.getType().equals("S")) {
				// simply insert into the storyEvents in order of ratio
				story = (StoryEvent) event;
				if(story.getHeight() > H) continue;
				ratio = story.getRatio();
				int j = 0;
				for(StoryEvent preStory : storyEvents) {
					if(preStory.getRatio() < ratio) {
						break;
					}
					j++;
				}
				storyEvents.add(j, story);
				recompute = true;
				if(debug) {
					System.out.println("Adding Story: " + story.getSid());
				}
				
				// we don't need to care about the mask for now. 
				// The score is what we care.
				//best.mask.add(j, false);
				if(story.getHeight() + best.height <= H) {
					best.height += story.getHeight();
					best.score += story.getScore();
					best.size++;
					best.mask.add(j, true);
				} else {
					best.mask.add(j, false);
				}
				//best.capacity++;
				curSolution.mask.add(curSolution.mask.size(), false);
			} else {
				// its a reload event
				
				// check the window
				// only when the story in best solution is removed should 
				// we recompute
				//storyIter = storyEvents.iterator();
				storyCount = 0;
				removeNum = 0;
				
				//int maskLen = best.mask.size();
				StoryEvent s = null;
				numStory = storyEvents.size();
				while(storyCount + removeNum < numStory) {
					s = storyEvents.get(storyCount);
					if(s.getTime() < event.getTime() - W) {
						storyEvents.remove(storyCount);

						if(best.mask.get(storyCount)) {
							best.height -= s.getHeight();
							best.score -= s.getScore();
							best.size--;
							//best.capacity--;
							recompute = true;
						}
						best.mask.remove(storyCount);
						curSolution.mask.remove(curSolution.mask.size()-1);
						removeNum++;

						if(debug) {
							System.out.println("Removing story: " + s.getSid());
						}
					} else {
						storyCount++;
					}
				}
				
				// get the best solution 
				if(debug) {
					System.out.println("Before findBest: Reload time: " + 
							event.getTime() + " height: " + best.height + 
							" score: " + best.score + " size: " + best.size);
				}
				if(recompute) {
					findBest(storyEvents, H);
					recompute = false;
					
					// retrive the sids
					sids.clear();
					//storyCount = best.capacity;
					storyCount = best.mask.size();
					for(int bitIdx=0; bitIdx<storyCount; bitIdx++) {
						if(best.mask.get(bitIdx)) {
							sids.add((storyEvents.get(bitIdx).getSid()));
						}
					}
					Collections.sort(sids);
				}
				
				System.out.print(best.score + " " + best.size);
				for(int sid : sids) {
					System.out.print(" " + sid);
				}
				System.out.print("\n");
				if(debug) {
					System.out.println("after findBest: height: " + best.height);
				}
			}
		}
		
	}
	
	private void findBest(ArrayList<StoryEvent> stories, int H) {
		/* based on horowitz_sahni algorithm. basically use binary bit mask to 
		 * denote if a story is pick and do backtracing on the story window. 
		 * Estimate the possible score up bound and do branch cut if the up bound
		 * is smaller than best score. the stories is re-ordered based on its 
		 * ratio for up bound estimate.
		 * return the best solution.
		 */
		
		int numStory = stories.size();
		int start = 0;
		int pos = 0;
		int end = 0;
		ArrayList<Integer> curSolutionSids = new ArrayList<Integer>();
		ArrayList<Integer> bestSids = new ArrayList<Integer>();
		int oldHeight = 0;
		int oldScore = 0;
		int oldSize = 0;
		int maskLen = 0;
		
		// clear curSolution
		curSolution.height = 0;
		curSolution.size = 0;
		curSolution.score = 0;
		Collections.fill(curSolution.mask, false);
		//curSolution.capacity = numStory;
		//best.capacity = numStory;
		
		if(numStory == 0) return;
		while(true) {
			pos = start;
			oldHeight = curSolution.height;
			oldSize = curSolution.size;
			oldScore = curSolution.score;
			if((end = findUpBound(stories, pos, numStory-1, curSolution, best, H)) < 0) {
				// the up bound is smaller than the best, so backtrace
				// first restore the state before explore
				
				curSolution.height = oldHeight;
				curSolution.size = oldSize;
				curSolution.score = oldScore;
				for(int k=-end; k>=start; k--) {
					curSolution.mask.set(k, false);
				}

				pos = backtracingOrExit(curSolution, start-1, stories);
				//pos = backtracingOrExit(curSolution, -end, stories);

				if(pos < 0) {
					return;
				} else {
					start = pos + 1;
				}
			
			} else if(end >= 0 && end < numStory - 1) {
				// the up bound is greater than the best, so continue to explore 
				start = end + 1;
			} else {
				// we have reached the end of the stories and the upbound is still greater 
				// than the best, then this might be the best
				// need to compare with the best
				if(curSolution.score > best.score || (curSolution.score == best.score && 
					curSolution.size < best.size)) {
					best.score = curSolution.score;
					best.size = curSolution.size;
					best.height = curSolution.height;
					Collections.copy(best.mask, curSolution.mask);
					//best.capacity = numStory;
				} else if(curSolution.score == best.score && curSolution.size == best.size){
					// the score and size are same, so need to see whose id is smaller
					getSolutionSid(curSolution, stories, curSolutionSids);
					getSolutionSid(best, stories, bestSids);
					Collections.sort(curSolutionSids);
					Collections.sort(bestSids);
					if(best.size > 0) {
						for(int idx=0; idx<curSolution.size; idx++) {
							if(curSolutionSids.get(idx) > bestSids.get(idx)) {
								break;
							} else if(curSolutionSids.get(idx) < bestSids.get(idx)) {
								best.score = curSolution.score;
								best.size = curSolution.size;
								best.height = curSolution.height;
								Collections.copy(best.mask, curSolution.mask);
								//best.capacity = numStory;
								break;
							}
						}
					} 
				}
				if(debug) {
					System.out.println("Best Solution so far: score: " + best.score + 
							" height: " + best.height);
				}
				
				// whether we find one solution or not, need to continue to explore
				//if(best.height == H) return;
				maskLen = curSolution.mask.size();
				if(curSolution.mask.get(maskLen-1)) {
					curSolution.height = oldHeight;
					curSolution.size = oldSize;
					curSolution.score = oldScore;
					for(int k=maskLen-1; k>=start; k--) {
						curSolution.mask.set(k, false);
					}
					pos = backtracingOrExit(curSolution, start-1, stories);
				} else {
					pos = backtracingOrExit(curSolution, maskLen-2, stories);
				}
				if(pos < 0) {
					return;
				} else {
					start = pos + 1;
				}
			}
		}
	}
	
	private int findUpBound(ArrayList<StoryEvent> stories, int start, int end, SolutionInfo cur, SolutionInfo best, int H) {
		/* find the up bound of the score from index start in stories
		 * return the index of the break point if up bound is greater than best, else return -1.
		 */
		int pos = start;
		StoryEvent s = null;
		int upBound = 0;
		int numStory = stories.size();
		//ListIterator<StoryEvent> iter = stories.listIterator(start);
		
		if(start >= stories.size()) return start;
		while(pos < numStory) {
			s = stories.get(pos);
			if(s.getHeight() > H - cur.height) {
				if(debug) {
					System.out.println("findUpBound: Not adding story: " + 
							s.getSid() + " pos: " + pos);
				}
				cur.mask.set(pos, false);

				break;
			} else {
				cur.height += s.getHeight();
				cur.score += s.getScore();
				cur.size++;
				cur.mask.set(pos, true);

				if(debug) {
					System.out.println("findUpBound: adding story: " + 
							s.getSid() + "height: " + cur.height + 
							"score: " + cur.score + "pos: " + pos);
				}
			}
			pos++;
		}
		
		if(cur.height == H) return end;
		
		if(pos >= end) {
			upBound = cur.score;
		} else {
			upBound = cur.score + (int) (s.getRatio() * (H - cur.height));
		}
		
		if(upBound >= best.score  || pos >= end) {
			return pos > end ? end : pos;
		} else {
			return -pos;
		}
	}
	
	private void getSolutionSid(SolutionInfo solution, ArrayList<StoryEvent> stories, ArrayList<Integer> sids) {
		/* from the solution mask, get the stories' sid 
		 * the sids are stored in sids
		 */
		int pos = 0;
		StoryEvent s = null;
		int maskLen = solution.mask.size();
		while(pos < maskLen) {
			s = stories.get(pos);
			if(solution.mask.get(pos)) {
				sids.add(s.getSid());
			}
			pos++;
		}
		
		return;
	}
	
	private int backtracingOrExit(SolutionInfo solution, int fromIdx, ArrayList<StoryEvent> stories) {
		/* find the first bit backward that is not set, from fromIdx inclusive
		 * return the index of that bit
		 */
		int pos = 0;
		StoryEvent s = null;
		
		if(fromIdx < 0) return -1;
		//pos = solution.mask.previousSetBit(fromIdx);
		pos = getPreviousSetBit(solution.mask, fromIdx);
		if(pos >= 0) {
			// unset this position and then explore
			solution.mask.set(pos, false);
			s = stories.get(pos);
			solution.height -= s.getHeight();
			solution.score -= s.getScore();
			solution.size -= 1;
			if(debug) {
				System.out.println("backtracing: unset story: " + s.getSid() + 
						"height: " + solution.height + "score: " + solution.score);
			}
		}
		
		if(debug) {
			if(pos < 0)
				System.out.println("backtracing: return to the header already.");
		}
	
		return pos;
	}
	
	private int getPreviousSetBit(ArrayList<Boolean> mask, int fromIdx) {
		/* return the index of previous story that's chosen */
		if(fromIdx < 0) return -1;
		int pos = fromIdx;
		while(pos >= 0 && !mask.get(pos)) {
			pos--;
		}
		
		return pos;		
	}
	
	public static void main(String args[]) throws Exception {
		// read in data
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		ArrayList<Event> events = new ArrayList<Event>();
		FeedOptimizer fo = null;

		
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
		fo = new FeedOptimizer(count);
		fo.solution(events, count, W, H);
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
	private float ratio;
	public StoryEvent(String t, int time, int score, int height, int sid) {
		super(t, time);
		this.score = score;
		this.height = height;
		this.sid = sid;
		ratio = ((float)score) / height;
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
	
	public float getRatio() {
		return ratio;
	}
}

class SolutionInfo {
	ArrayList<Boolean> mask;
	int size;
	int score;
	int height;
	//int capacity;
	
	public SolutionInfo(int N) {
		mask = new ArrayList<Boolean>(N);
		size = 0;
		score = 0;
		height = 0;
		//capacity = 0;
	}
}