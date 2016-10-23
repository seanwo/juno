package us.wohlgemuth;

public class Main {

    public static void main(String[] args) {
        Configuration config = new Configuration();
        Monitor monitor = new Monitor(config);
        monitor.run();
    }
}
