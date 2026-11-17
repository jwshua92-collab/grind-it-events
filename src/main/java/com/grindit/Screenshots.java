package com.grindit;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

final class Screenshots
{
	/** Big clients are scaled down so a screenshot stays well under the site's upload limit. */
	static final int MAX_WIDTH = 1920;

	private Screenshots()
	{
	}

	/** A copy of the frame, safe to keep after the draw callback returns. */
	static BufferedImage copy(Image frame)
	{
		int w = frame.getWidth(null);
		int h = frame.getHeight(null);
		if (w <= 0 || h <= 0)
		{
			return null;
		}
		BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = out.createGraphics();
		try
		{
			g.drawImage(frame, 0, 0, null);
		}
		finally
		{
			g.dispose();
		}
		return out;
	}

	static byte[] png(BufferedImage image) throws IOException
	{
		BufferedImage img = image;
		if (img.getWidth() > MAX_WIDTH)
		{
			int h = Math.max(1, Math.round(img.getHeight() * (MAX_WIDTH / (float) img.getWidth())));
			BufferedImage scaled = new BufferedImage(MAX_WIDTH, h, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = scaled.createGraphics();
			try
			{
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g.drawImage(img, 0, 0, MAX_WIDTH, h, null);
			}
			finally
			{
				g.dispose();
			}
			img = scaled;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(img, "png", out);
		return out.toByteArray();
	}
}
