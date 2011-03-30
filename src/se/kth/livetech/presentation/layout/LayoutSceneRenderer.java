package se.kth.livetech.presentation.layout;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.Set;
import java.util.TreeSet;

import se.kth.livetech.contest.graphics.ICPCColors;
import se.kth.livetech.contest.graphics.ICPCImages;
import se.kth.livetech.contest.graphics.RowFrameRenderer;
import se.kth.livetech.presentation.contest.ContestStyle;
import se.kth.livetech.presentation.graphics.ColoredTextBox;
import se.kth.livetech.presentation.graphics.ImageRenderer;
import se.kth.livetech.presentation.graphics.ImageResource;
import se.kth.livetech.presentation.graphics.Renderable;
import se.kth.livetech.util.DebugTrace;

public class LayoutSceneRenderer implements Renderable {
	public static final boolean DEBUG = true;
	
	ISceneLayout scene;
	
	public void updateScene(ISceneLayout update) {
		this.scene = update;
	}
	
	@Override
	public void render(Graphics2D g, Dimension d) {
		render(g, this.scene);
	}

	private void render(Graphics2D g, ISceneLayout scene) {
		Set<Integer> s = new TreeSet<Integer>();

		for (Object layer : scene.getLayers()) {
			s.add((Integer) layer);
			DebugTrace.trace("Layer1 " + layer);
		}
		for (int layer : s) {
			DebugTrace.trace("Layer2 " + layer);
			render(g, scene, layer);
		}
	}

	private void render(Graphics2D g, LayoutScene scene, Object layer) {
		if (!scene.getLayers().contains(layer)) {
			return;
		}

		AffineTransform at = g.getTransform();
		g.translate(scene.getBounds().getX(), scene.getBounds().getY());
		Content content = scene.getContent();
		if (DEBUG) {
			AffineTransform bt = g.getTransform();
			g.setTransform(at);
			g.setColor(Color.WHITE);
			g.draw(scene.getBounds());
			g.drawString("" + scene.getSubs().size() + '/' + scene.getLayers(), (int) scene.getBounds().getX(), (int) scene.getBounds().getY());
			g.setTransform(bt);
			//return;
		}
		if (content != null && content.getLayer() == (int) (Integer) layer) {
			Renderable r;
			if (content.isText()) {
				ContestStyle style = (ContestStyle) content.getStyle();
				r = new ColoredTextBox(content.getText(), ContestStyle.textBoxStyle(style));
			} else if (content.isImage()) {
				String imageName = content.getImageName();
				ImageResource image = ICPCImages.getResource(imageName);
				r = new ImageRenderer(imageName, image);
			} else {
				ContestStyle style = (ContestStyle) content.getStyle();
				Color row1 = ICPCColors.BG_COLOR_1;
				Color row2 = ICPCColors.BG_COLOR_2;
				if (style == ContestStyle.rowBackground1) {
					r = new RowFrameRenderer(row1, row2);
				} else {
					r = new RowFrameRenderer(row2, row1);
				}
			}
			Dimension d = new Dimension();
			// FIXME: Do not add .99!
			d.setSize(scene.getBounds().getWidth() + .99, scene.getBounds().getHeight() + .99);
			if (d.getWidth() > 0 && d.getHeight() > 0) {
				r.render(g, d);
			} else {
				// FIXME
			}
		}
		
		for (ISceneLayout sub : scene.getSubs()) {
			render(g, sub, layer);
		}
		g.setTransform(at);
	}
}
