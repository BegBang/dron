package cz.aimtec.hackathon.drone.views;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;

import cz.aimtec.hackathon.drone.drone.IFrameListener;
import com.parrot.arsdk.arcontroller.ARCONTROLLER_STREAM_CODEC_TYPE_ENUM;
import com.parrot.arsdk.arcontroller.ARControllerCodec;
import com.parrot.arsdk.arcontroller.ARFrame;

import java.nio.ByteBuffer;

public class StreamProcessingView extends TextureView implements TextureView.SurfaceTextureListener
{
    private static final String VIDEO_MIME_TYPE = "video/avc";
    private static final int VIDEO_DEQUEUE_TIMEOUT = 33000;
    private static final int VIDEO_WIDTH = 1280;
    private static final int VIDEO_HEIGHT = 720;
    private Surface surface;
    private MediaCodec mediaCodec;
    private boolean surfaceCreated = false;
    private boolean codecConfigured = false;
    private ByteBuffer[] buffers;

    //Sequence Parameter Set
    private ByteBuffer spsBuffer;
    //Picture Parameter Set
    private ByteBuffer ppsBuffer;

    protected long lastUpdateTime;

    protected IFrameListener frameListener;

    public StreamProcessingView(Context context)
    {
        this(context, null);
    }

    public StreamProcessingView(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public StreamProcessingView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        setSurfaceTextureListener(this);
        lastUpdateTime = 0;

        if (context instanceof IFrameListener)
        {
            this.frameListener = (IFrameListener) context;
        }
    }

    public void configureDecoder(ARControllerCodec codec)
    {
        if (codec.getType() == ARCONTROLLER_STREAM_CODEC_TYPE_ENUM.ARCONTROLLER_STREAM_CODEC_TYPE_H264)
        {
            ARControllerCodec.H264 codecH264 = codec.getAsH264();

            spsBuffer = ByteBuffer.wrap(codecH264.getSps().getByteData());
            ppsBuffer = ByteBuffer.wrap(codecH264.getPps().getByteData());
        }

        if ((mediaCodec != null) && (spsBuffer != null))
        {
            configureMediaCodec();
        }
    }


    public void displayFrame(ARFrame frame)
    {
        if (!surfaceCreated || spsBuffer == null)
        {
            return;
        }

        if (!codecConfigured)
        {
            configureMediaCodec();
        }

        // Here we have either a good PFrame, or an IFrame
        int index = -1;

        try
        {
            index = mediaCodec.dequeueInputBuffer(VIDEO_DEQUEUE_TIMEOUT);
        } catch (IllegalStateException e)
        {
            System.err.println("Error while dequeue input buffer");
        }

        if (index >= 0)
        {
            ByteBuffer b;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
            {
                b = mediaCodec.getInputBuffer(index);
            } else
            {
                b = buffers[index];
                b.clear();
            }

            if (b != null)
            {
                b.put(frame.getByteData(), 0, frame.getDataSize());
            }

            try
            {
                mediaCodec.queueInputBuffer(index, 0, frame.getDataSize(), 0, 0);
            } catch (IllegalStateException e)
            {
                System.err.println("Error while dequeue input buffer (outIndex)");
            }
        }

        // Try to display previous frame
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int outIndex;
        try
        {
            outIndex = mediaCodec.dequeueOutputBuffer(info, 0);

            while (outIndex >= 0)
            {
                mediaCodec.releaseOutputBuffer(outIndex, true);
                outIndex = mediaCodec.dequeueOutputBuffer(info, 0);
            }


        } catch (IllegalStateException e)
        {
            System.err.println("Error while dequeue input buffer (outIndex)");
        }
    }

    private void configureMediaCodec()
    {
        try
        {
            final MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT);
            format.setByteBuffer("csd-0", spsBuffer);
            format.setByteBuffer("csd-1", ppsBuffer);

            mediaCodec = MediaCodec.createDecoderByType(VIDEO_MIME_TYPE);
            mediaCodec.configure(format, surface, null, 0);
            mediaCodec.start();

            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP)
            {
                buffers = mediaCodec.getInputBuffers();
            }

            codecConfigured = true;
        } catch (Exception e)
        {
            System.err.println("Exception in configureMediaCodec");
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
    {
        this.surface = new Surface(surface);
        surfaceCreated = true;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
    {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface)
    {
        if (mediaCodec != null)
        {
            if (codecConfigured)
            {
                mediaCodec.stop();
                mediaCodec.release();
            }
            codecConfigured = false;
            mediaCodec = null;
        }

        if (surface != null)
        {
            surface.release();
        }

        if (this.surface != null)
        {
            this.surface.release();
        }

        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface)
    {
        if (this.frameListener.canWrite())
        {
            this.frameListener.setFrame(getBitmap());
        }
    }
}
