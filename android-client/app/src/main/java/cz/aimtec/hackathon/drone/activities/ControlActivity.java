package cz.aimtec.hackathon.drone.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import cz.aimtec.hackathon.drone.R;
import cz.aimtec.hackathon.drone.connectivity.SewioConnector;
import cz.aimtec.hackathon.drone.drone.DefaultBebopAdapter;
import cz.aimtec.hackathon.drone.drone.IBebopListener;

/**
 * Activity for testing
 * */
public class ControlActivity extends ADroneActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        drone.setMaxRotationSpeed(90);
        drone.setMaxVerticalSpeed(3);
        drone.setMaxTilt(20);

        SewioConnector connector = new SewioConnector();
        connector.getModels(new SewioConnector.AsyncSewioResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Object parsedJsonObject, String responseText) {
                makeToast("Received response: " + responseText);
            }
        });
    }

    @Override
    public Context getCurrentContext()
    {
        return ControlActivity.this;
    }

    public IBebopListener getBebopListener()
    {
        return bebopListener;
    }

    @Override
    protected IBebopListener initBebopListener()
    {
        return new DefaultBebopAdapter(this)
        {
            @Override
            public void onBatteryChargeChanged(int batteryPercentage)
            {
                ((TextView)findViewById(R.id.batteryView)).setText(String.format("%d%%", batteryPercentage));
            }

            @Override
            public void onMatchingMediasFound(int nbMedias)
            {
                mDownloadProgressDialog.dismiss();
                mNbMaxDownload = nbMedias;
                mCurrentDownloadIndex = 1;
                if (nbMedias > 0)
                {
                    mDownloadProgressDialog = new ProgressDialog(ControlActivity.this, R.style.ThemeOverlay_AppCompat_Dialog_Alert);
                    mDownloadProgressDialog.setIndeterminate(false);
                    mDownloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    mDownloadProgressDialog.setMessage("Downloading medias");
                    mDownloadProgressDialog.setMax(mNbMaxDownload * 100);
                    mDownloadProgressDialog.setSecondaryProgress(mCurrentDownloadIndex * 100);
                    mDownloadProgressDialog.setProgress(0);
                    mDownloadProgressDialog.setCancelable(false);
                    mDownloadProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            drone.cancelGetLastFlightMedias();
                        }
                    });
                    mDownloadProgressDialog.show();
                }
            }

            @Override
            public void onDownloadProgressed(String mediaName, int progress)
            {
                mDownloadProgressDialog.setProgress(((mCurrentDownloadIndex - 1) * 100) + progress);
            }

            @Override
            public void onDownloadComplete(String mediaName)
            {
                mCurrentDownloadIndex++;
                mDownloadProgressDialog.setSecondaryProgress(mCurrentDownloadIndex * 100);

                if (mCurrentDownloadIndex > mNbMaxDownload)
                {
                    mDownloadProgressDialog.dismiss();
                    mDownloadProgressDialog = null;
                }
            }
        };
    }
    //-----------------------------------------------------

    public void takeOffClick(View v)
    {
        System.out.println("TAKE OFF clicked");
        drone.takeOff();
    }

    /**
     * Sets drones roll parameters to half of maximum angle
     * @param v
     */
    public void rollLeft(View v)
    {
        System.out.println("TAKE OFF clicked");
        System.out.println("TAKE OFF clicked");
        drone.setRoll((byte)-50);
        drone.setFlag((byte) 1);
    }

    public void rollRight(View v)
    {
        drone.setRoll((byte)50);
        drone.setFlag((byte) 1);
    }

    public void rollStop(View v)
    {
        drone.setRoll((byte)0);
        drone.setFlag((byte) 0);
    }

    public void gazUp(View v)
    {
        drone.setGaz((byte)50);
    }

    public void gazDown(View v)
    {
        drone.setGaz((byte)-50);
    }

    public void gazZero(View v)
    {
        drone.setGaz((byte)0);
    }

    public void landClick(View v)
    {
        System.out.println("LAND clicked");
        drone.land();
    }

    public void rotateLeft(View v)
    {
        System.out.println("ROTATE LEFT clicked");
        //drone.turnLeft(Math.PI * 2);
        drone.turnLeft(6.2);
    }

    public void rotateRight(View v)
    {
        System.out.println("ROTATE RIGHT clicked");
        drone.turnRight(6.2);
        //drone.turnRight(Math.PI * 2);
    }

    public void moveUp(View v)
    {
        System.out.println("MOVE clicked");
        drone.moveUp(0.5);
    }

    public void moveDown(View v)
    {
        System.out.println("MOVE clicked");
        drone.moveDown(0.5);
    }

    public void moveLeft(View v)
    {
        System.out.println("MOVE clicked");
        drone.moveLeft(0.5);
    }

    public void moveRight(View v)
    {
        System.out.println("MOVE clicked");
        drone.moveRight(0.5);
    }

    public void moveForward(View v)
    {
        System.out.println("MOVE clicked");
        drone.moveForward(0.5);
    }

    public void moveBackward(View v)
    {
        System.out.println("MOVE clicked");
        drone.moveBackward(0.5);
    }

    /**
     * Turns off drones rotors and it falls down.
     * */
    public void emergency(View v)
    {
        drone.emergency();
    }

    /**
     * Sets current drones position as zero for gyroscope
     * Place drone on flat surface and trigger this buttons event
     * */
    public void flatTrimClick(View v)
    {
        drone.doFlatTrim();
    }

    /**
     * Do the photo
     * */
    public void photoClick(View v)
    {
        drone.takePicture();
    }

    public void downloadClick(View v)
    {
        drone.getLastFlightMedias();

        mDownloadProgressDialog = new ProgressDialog(ControlActivity.this, R.style.ThemeOverlay_AppCompat_Dialog_Alert);
        mDownloadProgressDialog.setIndeterminate(true);
        mDownloadProgressDialog.setMessage("Fetching medias");
        mDownloadProgressDialog.setCancelable(false);
        mDownloadProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                drone.cancelGetLastFlightMedias();
            }
        });
        mDownloadProgressDialog.show();
    }

}
