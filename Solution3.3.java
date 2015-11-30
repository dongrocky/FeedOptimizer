/* mask changed to array list, not working yet */

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.io.*;
import java.util.ListIterator;

public class FeedOptimizer {
	static boolean debug = true;
	public void solution(ArrayList<Event> events, int N, int W, int H) {
		LinkedList<StoryEvent> storyEvents = new LinkedList<StoryEvent>();
		SolutionInfo best = new SolutionInfo(0);
		SolutionInfo cur = new SolutionInfo(0);
		boolean recompute = true;
		Event event = null;
		StoryEvent story = null;
		//StoryEvent preStory = null;
		float ratio = 0;
		Iterator<StoryEvent> storyIter = null;
		int storyCount = 0;
		ArrayList<Integer> sids = new ArrayList<Integer>();
		int removeNum = 0;

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
				
				// we don't need to care about the mask for now. 
				// The score is what we care.
				/*
				if(story.getHeight() + best.height <= H) {
					best.height += story.getHeight();
					best.score += story.getScore();
					best.size++;
					best.mask.add(j, true);
				} else {
					best.mask.add(j, false);
				}
				*/
			} else {
				// its a reload event
				
				// check the window
				// only when the story in best solution is removed should 
				// we recompute
				storyIter = storyEvents.iterator();
				storyCount = 0;
				removeNum = 0;
				int maskLen = best.mask.size();
				StoryEvent s = null;
				while(storyIter.hasNext()) {
					s = storyIter.next();
					if(s.getTime() < event.getTime() - W) {
						storyIter.remove();
						if(storyCount + removeNum < maskLen) {
							if(best.mask.get(storyCount)) {
								best.height -= s.getHeight();
								best.score -= s.getScore();
								best.size--;
								recompute = true;
							}
							best.mask.remove(storyCount);
							removeNum++;
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
					best = findBest(storyEvents, H, best, cur);
					recompute = false;
					
					// retrive the sids
					sids.clear();
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
	
	private SolutionInfo findBest(LinkedList<StoryEvent> stories, int H, 
			SolutionInfo best, SolutionInfo curSolution) {
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
		curSolution.mask = new ArrayList<Boolean>(numStory);
		
		if(numStory == 0) return best;
		while(true) {
			pos = start;
			oldHeight = curSolution.height;
			oldSize = curSolution.size;
			oldScore = curSolution.score;
			if((end = findUpBound(stories, pos, numStory-1, curSolution, best, H)) < 0) {
				// the up bound is smaller than the best, so backtrace
				// first restore the state before explore
				if(curSolution.mask.get(-end)) {
					curSolution.height = oldHeight;
					curSolution.size = oldSize;
					curSolution.score = oldScore;
					for(int k=-end; k>=start; k--) {
						curSolution.mask.set(k, false);
					}
					pos = backtracingOrExit(curSolution, start-1, stories);
				} else {
					pos = backtracingOrExit(curSolution, -end-1, stories);
				}
				if(pos < 0) {
					return best;
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
					best.mask = new ArrayList<Boolean>(curSolution.mask);
				} else {
					// the score and size are same, so need to see whose id is smaller
					getSolutionSid(curSolution, stories, curSolutionSids);
					getSolutionSid(best, stories, bestSids);
					Collections.sort(curSolutionSids);
					Collections.sort(bestSids);
					if(bestSids.size() > 0) {
						for(int idx=0; idx<curSolution.size; idx++) {
							if(curSolutionSids.get(idx) > bestSids.get(idx)) {
								break;
							} else if(curSolutionSids.get(idx) < bestSids.get(idx)) {
								best.score = curSolution.score;
								best.size = curSolution.size;
								best.height = curSolution.height;
								best.mask = new ArrayList<Boolean>(curSolution.mask);
								break;
							}
						}
					} else {
						best.score = curSolution.score;
						best.size = curSolution.size;
						best.height = curSolution.height;
						best.mask = new ArrayList<Boolean>(curSolution.mask);
					}
				}
				
				// whether we find one solution or not, need to continue to explore
				if(curSolution.height == H || curSolution.mask.get(numStory-1)) {
					curSolution.height = oldHeight;
					curSolution.size = oldSize;
					curSolution.score = oldScore;
					maskLen = curSolution.mask.size();
					for(int k=maskLen-1; k>=start; k--) {
						curSolution.mask.set(k, false);
					}
					pos = backtracingOrExit(curSolution, start-1, stories);
				} else {
					pos = backtracingOrExit(curSolution, -end, stories);
				}
				if(pos < 0) {
					return best;
				} else {
					start = pos + 1;
				}
			}
		}
	}
	
	private int findUpBound(LinkedList<StoryEvent> stories, int start, int end, SolutionInfo cur, SolutionInfo best, int H) {
		/* find the up bound of the score from index start in stories
		 * return the index of the break point if up bound is greater than best, else return -1.
		 */
		int pos = start-1;
		StoryEvent s = null;
		int upBound = 0;
		ListIterator<StoryEvent> iter = stories.listIterator(start);
		
		if(start >= stories.size()) return start;
		while(iter.hasNext()) {
			pos++;
			s = iter.next();
			if(s.getHeight() > H - cur.height) {
				if(pos >= cur.mask.size()) {
					cur.mask.add(pos, false);
				} else {
					cur.mask.set(pos, false);
				}
				break;
			} else {
				cur.height += s.getHeight();
				cur.score += s.getScore();
				cur.size++;
				if(pos >= cur.mask.size()) {
					cur.mask.add(pos, true);
				} else {
					cur.mask.set(pos, true);
				}
			}
		}
		
		if(cur.height == H) return end;
		
		if(pos >= end) {
			upBound = cur.score;
		} else {
			upBound = cur.score + (int) (s.getRatio() * (H - cur.height));
		}
		
		if(upBound >= best.score) {
			return end;
		} else {
			return -pos;
		}
	}
	
	private void getSolutionSid(SolutionInfo solution, LinkedList<StoryEvent> stories, ArrayList<Integer> sids) {
		/* from the solution mask, get the stories' sid 
		 * the sids are stored in sids
		 */
		int pos = -1;
		StoryEvent s = null;
		Iterator<StoryEvent> iter = stories.iterator();
		int maskLen = solution.mask.size();
		while(iter.hasNext() && pos < maskLen - 1) {
			s = iter.next();
			pos++;
			if(solution.mask.get(pos)) {
				sids.add(s.getSid());
			}
		}
		
		return;
	}
	
	private int backtracingOrExit(SolutionInfo solution, int fromIdx, LinkedList<StoryEvent> stories) {
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
		}
	
		return pos;
	}
	
	private int getPreviousSetBit(ArrayList<Boolean> mask, int fromIdx) {
		/* return the index of previous story that's chosen */
		if(fromIdx < 0) return -1;
		int len = mask.size();	
		int pos = Math.min(fromIdx, len-1);
		while(pos >= 0 && !mask.get(pos)) {
			pos--;
		}
		
		return pos;		
	}
	
	public static void main(String args[]) throws Exception {
		// read in data
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		ArrayList<Event> events = new ArrayList<Event>();
		FeedOptimizer fo = new FeedOptimizer();
		
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
	
	public SolutionInfo(int N) {
		mask = new ArrayList<Boolean>(N);
		size = 0;
		score = 0;
		height = 0;
	}
}