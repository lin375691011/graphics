package se.kth.livetech.presentation.layout;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JPanel;

import se.kth.livetech.contest.graphics.ContentProvider;
import se.kth.livetech.contest.graphics.ICPCColors;
import se.kth.livetech.contest.graphics.ICPCImages;
import se.kth.livetech.contest.graphics.RowFrameRenderer;
import se.kth.livetech.contest.graphics.TestcaseStatusRenderer;
import se.kth.livetech.contest.model.ContestUpdateEvent;
import se.kth.livetech.contest.model.ContestUpdateListener;
import se.kth.livetech.contest.model.Run;
import se.kth.livetech.contest.model.Testcase;
import se.kth.livetech.contest.model.test.FakeContest;
import se.kth.livetech.contest.model.test.TestContest;
import se.kth.livetech.contest.replay.ContestReplay;
import se.kth.livetech.presentation.animation.AnimationStack;
import se.kth.livetech.presentation.animation.Interpolated;
import se.kth.livetech.presentation.graphics.ColoredTextBox;
import se.kth.livetech.presentation.graphics.ImageRenderer;
import se.kth.livetech.presentation.graphics.ImageResource;
import se.kth.livetech.presentation.graphics.PartitionedRowRenderer;
import se.kth.livetech.presentation.graphics.Renderable;
import se.kth.livetech.util.Frame;

@SuppressWarnings("serial")
public class JudgeQueueTest extends JPanel implements ContestUpdateListener {
	public static final boolean FULL_SCREEN = false;
	public static final double ANIMATION_TIME = 1000; // ms
	public static final double KEEP_TIME = 10000; // ms
	final int N = 20;
	final int P = 2;
	final int T = 55;

	Map<Integer, JudgeState> state = Collections.synchronizedMap(new TreeMap<Integer, JudgeState>());

	private static class JudgeState {
		TestcaseStatusRenderer.Status compiling, running, validating;
		TestcaseStatusRenderer.Status[] cases;
		long lastUpdateTime;
		public JudgeState() {
			compiling = TestcaseStatusRenderer.Status.active;
			running = TestcaseStatusRenderer.Status.none;
			validating = TestcaseStatusRenderer.Status.none;
		}

		public void update(Testcase testcase) {
			compiling = TestcaseStatusRenderer.Status.passed;
			running = TestcaseStatusRenderer.Status.active;
			int n = testcase.getN();
			if (cases == null) {
				cases = new TestcaseStatusRenderer.Status[n];
				for (int i = 0; i < n; ++i)
					cases[i] = TestcaseStatusRenderer.Status.none;
			}
			int i = testcase.getI() - 1;
			if (testcase.isJudged()) {
				if (testcase.isSolved()) {
					cases[i] = TestcaseStatusRenderer.Status.passed;
				}
				else {
					cases[i] = TestcaseStatusRenderer.Status.failed;
				}
			}
			else {
				cases[i] = TestcaseStatusRenderer.Status.active;
			}
			lastUpdateTime = System.currentTimeMillis();
		}

		public void update(Run run) {
			if (run.isJudged()) {
				compiling = TestcaseStatusRenderer.Status.passed;
				running = TestcaseStatusRenderer.Status.passed;
				if(run.isSolved()) {
					validating = TestcaseStatusRenderer.Status.passed;
				}
				else {
					validating = TestcaseStatusRenderer.Status.failed;
				}
			}
			lastUpdateTime = System.currentTimeMillis();
		}
	}

	public JudgeQueueTest() {
		this.setBackground(Color.BLUE.darker());
		this.setPreferredSize(new Dimension(512, 576));
	}

	AnimationStack<Integer, Integer> stack = new AnimationStack<Integer, Integer>();
	boolean firstPaint = true;
	long lastTime;
	public void paintComponent(Graphics gr) {
		super.paintComponent(gr);
		Graphics2D g = (Graphics2D) gr;

		long now = System.currentTimeMillis();

		boolean update = false;
		{ // Advance
			if (firstPaint) {
				this.lastTime = now;
				firstPaint = false;
			}
			long dt = now - this.lastTime;
			this.lastTime = now;
			update |= this.stack.advance(dt / ANIMATION_TIME);
			//update |= this.recent.advance(dt / RECENT_TIME);
		}

		Rectangle2D rect = Rect.screenRect(getWidth(), getHeight(), .03);
		Rectangle2D row = new Rectangle2D.Double();
		Dimension dim = new Dimension();
		int rowNumber = 0;
		Map<Integer, JudgeState> state;
		synchronized (this.state) {
			state = new TreeMap<Integer, JudgeState>(this.state);
		}
		for (int i : state.keySet()) {
			if(now - state.get(i).lastUpdateTime > KEEP_TIME)
				synchronized (this.state) {
					this.state.remove(i);
				}
		}
		synchronized (this.state) {
			state = new TreeMap<Integer, JudgeState>(this.state);
		}
		for (int i : state.keySet()) {
			PartitionedRowRenderer r = new PartitionedRowRenderer();

			{ // Background
				Color row1 = ICPCColors.BG_COLOR_1;
				Color row2 = ICPCColors.BG_COLOR_2;
				if (i % 2 == 0)
					r.setBackground(new RowFrameRenderer(row1, row2));
				else
					r.setBackground(new RowFrameRenderer(row2, row1));
			}

			{ // Flag
				String country = ICPCImages.COUNTRY_CODES[i % ICPCImages.COUNTRY_CODES.length];
				ImageResource image = ICPCImages.getFlag(country);
				Renderable flag = new ImageRenderer("flag " + country, image);
				r.add(flag, 1, .9, true);
			}

			{ // Logo
				ImageResource image = ICPCImages.getTeamLogo(i);
				Renderable logo = new ImageRenderer("logo " + i, image);
				r.add(logo, 1, .9, true);
			}

			{ // Team name
				// TODO: team name should be in a TeamSubmissionState...
				Renderable teamName = new ColoredTextBox("University " + i, ContentProvider.getTeamNameStyle());
				r.add(teamName, 1, 1, false);
			}

			// Testcases
			{ // States
				JudgeState js = state.get(i);
				Renderable testcase = new TestcaseStatusRenderer(js.compiling);
				r.add(testcase, 1, .95, true);
				Renderable testcaseRunning = new TestcaseStatusRenderer(js.running);
				r.add(testcaseRunning, 1, .95, true);
				Renderable testcaseValidating = new TestcaseStatusRenderer(js.validating);
				r.add(testcaseValidating, 1, .95, true);
			}

			{ // Render
				Interpolated.Double interpolator = new Interpolated.Double(rowNumber);
				stack.setPosition(i, rowNumber);
				stack.interpolate(i, interpolator);
				double rowPos = interpolator.getValue();
				Rect.setRow(rect, rowPos, N, row);
				Rect.setDim(row, dim);
				int x = (int) row.getX();
				int y = (int) row.getY();
				g.translate(x, y);
				r.render(g, dim);
				g.translate(-x, -y);
			}
			++rowNumber;
		}

		{ // Update?
			update |= this.stack.advance(0d);
			if (update) {
				repaint();
			}
		}
	}

	public static void main(String[] args) {
		final int teams = 100, problems = 12;
		TestContest tc = new TestContest(teams, problems, 0);
		FakeContest fc = new FakeContest(tc);
		JudgeQueueTest jqt = new JudgeQueueTest();
		//fc.addContestUpdateListener(jqt);
		ContestReplay cr = new ContestReplay();
		tc.addAttrsUpdateListener(cr);
		cr.addContestUpdateListener(jqt);
		Frame frame = new Frame("JudgeQueueTest", jqt, null, false);
		fc.start();
		if (FULL_SCREEN) {
			frame.fullScreen(0);
		}
		else {
			frame.pack();
			frame.setVisible(true);
		}
	}

	@Override
	public void contestUpdated(ContestUpdateEvent e) {
		if (e.getUpdate() instanceof Run) {
			Run run = (Run) e.getUpdate();
			if (!state.containsKey(run.getId())) {
				state.put(run.getId(), new JudgeState());
				// TODO: smooth insertion during previous animation...
				stack.setPosition(run.getId(), state.size() + 1);
			}
			JudgeState state = this.state.get(run.getId());
			if (state != null) {
				state.update(run);
			}
			repaint();
		}
		else if (e.getUpdate() instanceof Testcase) {
			Testcase testcase = (Testcase) e.getUpdate();
			JudgeState state = this.state.get(testcase.getRunId());
			if (state != null) {
				// TODO: look at testcase.isJudged/Solved
				state.update(testcase);
			}
			repaint();
		}
	}
}
