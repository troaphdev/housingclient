package com.housingclient.utils;

public class AnimationUtils {
    
    private double value;
    private double targetValue;
    private double speed;
    private long lastUpdate;
    
    public AnimationUtils(double initialValue, double speed) {
        this.value = initialValue;
        this.targetValue = initialValue;
        this.speed = speed;
        this.lastUpdate = System.currentTimeMillis();
    }
    
    public void update() {
        long currentTime = System.currentTimeMillis();
        double delta = (currentTime - lastUpdate) / 1000.0;
        lastUpdate = currentTime;
        
        if (value < targetValue) {
            value = Math.min(value + speed * delta, targetValue);
        } else if (value > targetValue) {
            value = Math.max(value - speed * delta, targetValue);
        }
    }
    
    public double getValue() {
        update();
        return value;
    }
    
    public void setTarget(double target) {
        this.targetValue = target;
    }
    
    public void setValue(double value) {
        this.value = value;
        this.targetValue = value;
    }
    
    public void setSpeed(double speed) {
        this.speed = speed;
    }
    
    public boolean isComplete() {
        return Math.abs(value - targetValue) < 0.001;
    }
    
    // Static animation methods
    public static double easeInOut(double t) {
        return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
    }
    
    public static double easeIn(double t) {
        return t * t;
    }
    
    public static double easeOut(double t) {
        return 1 - (1 - t) * (1 - t);
    }
    
    public static double easeInOutCubic(double t) {
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }
    
    public static double easeOutElastic(double t) {
        double c4 = (2 * Math.PI) / 3;
        return t == 0 ? 0 : t == 1 ? 1 : Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * c4) + 1;
    }
    
    public static double easeOutBounce(double t) {
        double n1 = 7.5625;
        double d1 = 2.75;
        
        if (t < 1 / d1) {
            return n1 * t * t;
        } else if (t < 2 / d1) {
            return n1 * (t -= 1.5 / d1) * t + 0.75;
        } else if (t < 2.5 / d1) {
            return n1 * (t -= 2.25 / d1) * t + 0.9375;
        } else {
            return n1 * (t -= 2.625 / d1) * t + 0.984375;
        }
    }
    
    public static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }
    
    public static float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }
    
    public static int lerpColor(int color1, int color2, float t) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int a = (int) lerp(a1, a2, t);
        int r = (int) lerp(r1, r2, t);
        int g = (int) lerp(g1, g2, t);
        int b = (int) lerp(b1, b2, t);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
    
    public static double smoothStep(double edge0, double edge1, double x) {
        double t = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
        return t * t * (3 - 2 * t);
    }
    
    public static double spring(double current, double target, double velocity, double stiffness, double damping, double delta) {
        double displacement = current - target;
        double springForce = -stiffness * displacement;
        double dampingForce = -damping * velocity;
        double acceleration = springForce + dampingForce;
        return velocity + acceleration * delta;
    }
}

