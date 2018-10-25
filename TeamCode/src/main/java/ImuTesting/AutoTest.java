package ImuTesting;
import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.Func;
import org.firstinspires.ftc.robotcore.external.navigation.Acceleration;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.robotcore.external.navigation.Position;
import org.firstinspires.ftc.robotcore.external.navigation.Velocity;

import java.util.Locale;


@Autonomous(name = "autoTest", group = "Auto")
public class AutoTest extends LinearOpMode {
    public DcMotor motorL;
    public DcMotor motorR;
    public Servo lServo;
    public Servo rServo;
    public DistanceSensor sideRange1;
    public DistanceSensor sideRange2;
    public DistanceSensor frontSensor;
    public Servo marker;

    BNO055IMU imu;

    Orientation angles;
    Acceleration gravity;


    enum HangState{
        LOWER,
        LTWIST,
        WIGGLE,
        RTWIST,
        STOP;

        public HangState getNext(){
            return this.ordinal()<HangState.values().length-1?HangState.values()[this.ordinal()+1]:HangState.LOWER;
        }

    }
    HangState hangState=HangState.LOWER;

    private boolean lower() throws InterruptedException{

        switch(hangState){
            case LOWER:
                lServo.setPosition(.1);
                rServo.setPosition(.1);

                Thread.sleep(2500);
                lServo.setPosition(.5);
                rServo.setPosition(.5);
                hangState= hangState.getNext();

                break;
            case LTWIST:
                if(turnDegrees(.3,-20)){
                    hangState= hangState.getNext();

                }
                break;
            case WIGGLE:
                if(driveTicks(.7,700)){
                    hangState= hangState.getNext();

                }
                break;
            case RTWIST:
                if(turnDegrees(.3,20)) {
                    hangState = hangState.getNext();
                }

                    break;
            case STOP:
            default:
                return true;
        }





        return false;
    }


    enum State{
        DETACH,
        MOVEBIT,
        FACEWALL,
        DELAYTIME,
        GOTOWALL,
        FACEDEPOT,
        GOTOWALL2,
        DEPOSIT,
        FACECRATER,
        DRIVECRATER,
        STOP;

        public State getNext(){
            return this.ordinal()<State.values().length-1?State.values()[this.ordinal()+1]:State.DETACH;
        }

    }
    State state=State.DETACH;

    int encoderTickInitial=0;
    double headingInitial=0;

    int initialRange=10;

    double angleTolerance=3;
    int goodCounter=0;
    int angleTimeTolerance=1000;

    void motorDrive(double power){
        motorL.setPower(-power);
        motorR.setPower(power);

    }
    private boolean driveTicks(double power, int ticks){
        motorDrive((ticks>0?1:-1)*power);
        return (ticks>0?motorR.getCurrentPosition()-encoderTickInitial>=ticks:motorR.getCurrentPosition()-encoderTickInitial<=ticks);
    }

    private boolean turnDegrees(double power, int degrees){
        double diff=(double)angles.firstAngle-headingInitial-degrees;

        if(Math.abs(diff)>angleTolerance){
            goodCounter=0;
            motorR.setPower((diff>0?1:-1)*power);

            if(Math.abs(diff)>8)
                motorL.setPower((diff>0?1:-1)*power);
            else motorL.setPower(0);
        }else{
            motorL.setPower(0);
            motorR.setPower(0);
            goodCounter++;
        }

        return goodCounter>=angleTimeTolerance;

        //return (degrees>0?(double)angles.firstAngle-headingInitial>=degrees:(double)angles.firstAngle-headingInitial<=degrees);


    }
    private void next() {
        state=state.getNext();
        encoderTickInitial=motorR.getCurrentPosition();
        headingInitial=(double)angles.firstAngle;
        motorDrive(0);
        goodCounter=0;
        initialRange=(int)sideRange1.getDistance(DistanceUnit.CM);
    }

    private void driveTime(double power, long time) throws InterruptedException {
        motorL.setPower(power);
        motorR.setPower(power);
        Thread.sleep(time);
        motorL.setPower(0);
        motorR.setPower(0);

    }


    int skatingTolerance=3;
    double skatingDiff=.35;
    private void skate(double power){
        int d1=(int)sideRange1.getDistance(DistanceUnit.CM);
        int d2=(int)sideRange2.getDistance(DistanceUnit.CM);

        //backward is r- l+
        //if port 0 is larger then r becomes more neg else vice

        if(power>0){
            double r=-power;
            double l=power;
            if(d1-d2-skatingTolerance>0)
                r-=skatingDiff;
            if(d2-d1-skatingTolerance>0)
                r+=skatingDiff;

            motorR.setPower(r);
            motorL.setPower(l);


        }else{
            double r=-power;
            double l=power;
            if(d1-d2-skatingTolerance>0)
                r-=skatingDiff;
            if(d2-d1-skatingTolerance>0)
                r+=skatingDiff;

            motorR.setPower(r);
            motorL.setPower(l);
        }




    }
    private boolean skateDist(double power, int dist){
        skate(power);
        if(dist>0){
            return motorR.getCurrentPosition()-encoderTickInitial>dist-encoderTickInitial;
        }
        return motorR.getCurrentPosition()-encoderTickInitial<dist-encoderTickInitial;
    }




//TODO:=============================================================================================
    boolean startHanging=true;
    boolean startNearCrater=true;
    boolean ownCrater =true;
    double timeDelay=.5;
//TODO:=============================================================================================

    @Override
    public void runOpMode() throws InterruptedException {
        motorL = hardwareMap.get(DcMotor.class, "leftMotor");
        motorR = hardwareMap.get(DcMotor.class, "rightMotor");
        lServo=hardwareMap.get(Servo.class,"lServo");
        rServo=hardwareMap.get(Servo.class,"rServo");

        marker=hardwareMap.get(Servo.class, "marker");


        lServo.setDirection(Servo.Direction.REVERSE);
        rServo.setDirection(Servo.Direction.FORWARD);

        frontSensor=hardwareMap.get(DistanceSensor.class, "front");
        sideRange1=hardwareMap.get(DistanceSensor.class, "sideRange1");
        sideRange2=hardwareMap.get(DistanceSensor.class, "sideRange2");

        motorL.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorR.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        motorL.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        motorR.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        imu = hardwareMap.get(BNO055IMU.class, "imu");
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.angleUnit           = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit           = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.calibrationDataFile = "BNO055IMUCalibration.json"; // see the calibration sample opmode
        parameters.loggingEnabled      = true;
        parameters.loggingTag          = "IMU";
        parameters.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();


        imu.initialize(parameters);
        imu.startAccelerationIntegration(new Position(), new Velocity(), 1000);


        boolean dDown=false;
        boolean dUp=false;
        boolean aDown=false;
        boolean bDown=false;
        boolean xDown=false;

        boolean isReady=false;

        composeTelemetry2();

        while(!isStarted()||!isReady){
            telemetry.update();
            if(gamepad1.dpad_down){dDown=true;}
            if(gamepad1.dpad_up){dUp=true;}
            if(gamepad1.a){aDown=true;}
            if(gamepad1.b){bDown=true;}
            if(gamepad1.x){xDown=true;}

            if(!gamepad1.dpad_down&&dDown){dDown=false;timeDelay-=.5;timeDelay=Math.abs(timeDelay);}
            if(!gamepad1.dpad_up&&dUp){dUp=false;timeDelay+=.5;}
            if(!gamepad1.a&&aDown){aDown=false;startHanging=!startHanging;}
            if(!gamepad1.b&&bDown){bDown=false;startNearCrater=!startNearCrater;}
            if(!gamepad1.x&&xDown){xDown=false;ownCrater=!ownCrater;}

            if(gamepad1.y){

                isReady=true;
                telemetry.clearAll();
                readyTelemetry();
                telemetry.update();
                break;
            }


        }

        waitForStart();

        telemetry.clearAll();
        composeTelemetry();
        marker.setPosition(1.1);

        while(!isStopRequested()){
            telemetry.update();

            switch(state){
                case DETACH:

                    if(startHanging){
                        if(lower()){
                            next();
                        }

                    }else {
                        next();
                    }
                    break;
                case MOVEBIT:
                    if(driveTicks(.7,-1500)){
                        next();

                    }
                    break;
                case FACEWALL:
                    if(startNearCrater){
                        if(turnDegrees(.3,90)){
                            next();
                        }
                    }else{
                        if(turnDegrees(.25,-45)){
                            next();
                        }
                    }
                    break;
                case DELAYTIME:
                    Thread.sleep((int)(timeDelay*1000));
                    next();
                    break;
                case GOTOWALL:
                    motorDrive(-.7);
                    if(frontSensor.getDistance(DistanceUnit.CM)<30){
                        next();
                    }
                    break;
                case FACEDEPOT:
                    if(startNearCrater) {
                        if (turnDegrees(.3, 45)) {
                            next();
                        }
                    }else{
                        if (turnDegrees(.3, 90)) {
                            next();
                        }
                    }
                    break;
                case GOTOWALL2:
                    skate(.6);
                    if(frontSensor.getDistance(DistanceUnit.CM)<70){
                        next();
                    }
                    break;
                case DEPOSIT:
                    marker.setPosition(.2);
                    Thread.sleep(1000);
                    imu.initialize(parameters);
                    imu.startAccelerationIntegration(new Position(), new Velocity(), 1000);
                    next();


                    break;
                case FACECRATER:
                    if(!ownCrater) {
                        if (turnDegrees(.4, 90)) {
                            next();
                        }
                    }else{
                        next();
                    }

                    break;
                case DRIVECRATER:
                    if(ownCrater){
                        skate(-.7);
                        if(angles.secondAngle>5||angles.secondAngle<-5){
                            next();
                        }
                    }else{
                        skate(.7);
                        next();
                        //TODO: FIX MEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE

                    }
                    break;
                case STOP:
                default:
                    requestOpModeStop();

            }

        }





    }




    void composeTelemetry() {
        telemetry.clearAll();

        // At the beginning of each telemetry update, grab a bunch of data
        // from the IMU that we will then display in separate lines.
        telemetry.addAction(new Runnable() { @Override public void run()
        {
            // Acquiring the angles is relatively expensive; we don't want
            // to do that in each of the three items that need that info, as that's
            // three times the necessary expense.
            angles   = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);
            gravity  = imu.getGravity();
        }
        });

        telemetry.addLine()
                .addData("Gamepad L/R:", new Func<String>() {
                    @Override public String value() {
                        return gamepad1.left_stick_y+"/"+gamepad1.right_stick_y;
                    }
                })
                .addData("Power L/R:", new Func<String>() {
                    @Override public String value() {
                        return motorL.getPower()+"/"+motorR.getPower();
                    }
                });

        telemetry.addLine()
                .addData("heading", new Func<String>() {
                    @Override public String value() {
                        return formatAngle(angles.angleUnit, angles.firstAngle);
                    }
                })
                .addData("roll", new Func<String>() {
                    @Override public String value() {
                        return formatAngle(angles.angleUnit, angles.secondAngle);
                    }
                })
                .addData("pitch", new Func<String>() {
                    @Override public String value() {
                        return formatAngle(angles.angleUnit, angles.thirdAngle);
                    }
                });

        telemetry.addLine()
                .addData("State: ", new Func<String>() {
                    @Override public String value() {
                        if(state!=State.DETACH)
                            return state+"";
                        return "DETACH: "+hangState;
                    }
                })
                .addData("mag", new Func<String>() {
                    @Override public String value() {
                        return String.format(Locale.getDefault(), "%.3f",
                                Math.sqrt(gravity.xAccel*gravity.xAccel
                                        + gravity.yAccel*gravity.yAccel
                                        + gravity.zAccel*gravity.zAccel));
                    }
                });
        telemetry.addLine()
                .addData("enc1", new Func<String>() {
                    @Override public String value() {
                        return motorL.getCurrentPosition()+"";
                    }
                })
                .addData("encR", new Func<String>() {
                    @Override public String value() {
                        return ""+motorR.getCurrentPosition();
                    }
                });
        telemetry.addLine()
                .addData("FramesWithinTolerance:", new Func<String>() {
                    @Override public String value() {
                        return goodCounter+"/"+angleTimeTolerance;
                    }
                })
                .addData("AngleDistance", new Func<String>() {
                    @Override public String value() {
                        return Math.abs((double)angles.firstAngle-headingInitial-45)+"";
                    }
                });
    }

    String line1="Initializing Autonomous... Press <Y> to finish!";
    String line2="<A> Start hanging?";
    String line3="<X> End at own crater? ";
    String line4="<B> Start near crater? ";
    String line5="<DPAD> Delay time ";

    void composeTelemetry2() {
        telemetry.clearAll();
        // At the beginning of each telemetry update, grab a bunch of data
        // from the IMU that we will then display in separate lines.
        telemetry.addAction(new Runnable() { @Override public void run()
        {
            // Acquiring the angles is relatively expensive; we don't want
            // to do that in each of the three items that need that info, as that's
            // three times the necessary expense.

        }
        });

        telemetry.addLine()
                .addData(line1, new Func<String>() {
                    @Override public String value() {
                        return "";
                    }
                });
        telemetry.addLine();
        telemetry.addLine()
                .addData(line2, new Func<String>() {
                    @Override public String value() {
                        return startHanging?"Yes":"No";
                    }
                });
        telemetry.addLine()
                .addData(line3, new Func<String>() {
                    @Override public String value() {

                        return ownCrater?"Yes":"No";

                    }
                });
        telemetry.addLine()
                .addData(line4, new Func<String>() {
                    @Override public String value() {
                        return startNearCrater?"Yes":"No";
                    }
                });
        telemetry.addLine()
                .addData(line5, new Func<String>() {
                    @Override public String value() {
                        return timeDelay+"";
                    }
                });


    }
    void readyTelemetry(){
        telemetry.clearAll();
        // At the beginning of each telemetry update, grab a bunch of data
        // from the IMU that we will then display in separate lines.
        telemetry.addAction(new Runnable() { @Override public void run()
        {


        }
        });

        telemetry.addLine()
                .addData("Autonomous ready!", new Func<String>() {
                    @Override public String value() {
                        return "";
                    }
                });
    }

    String formatAngle(AngleUnit angleUnit, double angle) {
        return formatDegrees(AngleUnit.DEGREES.fromUnit(angleUnit, angle));
    }

    String formatDegrees(double degrees){
        return String.format(Locale.getDefault(), "%.1f", AngleUnit.DEGREES.normalize(degrees));
    }
}
