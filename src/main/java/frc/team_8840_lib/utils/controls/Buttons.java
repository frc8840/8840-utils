package frc.team_8840_lib.utils.controls;

public class Buttons {
    //Literally "POV" but with char codes lol
    private static final int POVCode = 807986;

    public static class POV {
        public static final int Still = -1;
        public static final int UpRight = 135;
        public static final int Up = 180;
        public static final int UpLeft = 225;
        public static final int Left = 270;
        public static final int DownLeft = 315;
        public static final int Down = 0;
        public static final int DownRight = 45;
        public static final int Right = 90;
    }

    public static class Joystick {
        public static final int Trigger = 1;
        public static final int SideButton = 2;
        public static final int TopButton_3 = 3;
        public static final int TopButton_4 = 4;
        public static final int TopButton_5 = 5;
        public static final int TopButton_6 = 6;
        public static final int SideButton_7 = 7;
        public static final int SideButton_8 = 8;
        public static final int SideButton_9 = 9;
        public static final int SideButton_10 = 10;
        public static final int SideButton_11 = 11;
        public static final int SideButton_12 = 12;

        public static final int JoystickXAxis = 0;
        public static final int JoystickYAxis = 1;
        public static final int JoystickTwistAxis = 2;

        public static final int SliderAxis = 3;

        public static final int Top = POVCode;
    }

    public static class Xbox {
        public static final int A_Button = 1;
        public static final int B_Button = 2;
        public static final int X_Button = 3;
        public static final int Y_Button = 4;
        public static final int LB_Button = 5;
        public static final int RB_Button = 6;

        public static final int Back_Button = 7;
        public static final int Start_Button = 8;
        public static final int L3_Button = 9;
        public static final int R3_Button = 10;

        public static final int leftXAxis = 0;
        public static final int leftYAxis = 1;
        public static final int rightXAxis = 4;
        public static final int rightYAxis = 5;

        public static final int leftTrigger = 2;
        public static final int rightTrigger = 3;

        public static final int Arrows = POVCode;
    }
}
