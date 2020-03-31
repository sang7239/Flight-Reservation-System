package flightapp;

public class Flight {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;

    public Flight(int fid, int dayOfMonth, String carrierId, String flightNum, String originCity, String destCity,
                  int time, int capacity, int price) {
        this.fid = fid;
        this.dayOfMonth = dayOfMonth;
        this.carrierId = carrierId;
        this.flightNum = flightNum;
        this.originCity = originCity;
        this.destCity = destCity;
        this.time = time;
        this.capacity = capacity;
        this.price = price;
    }
    @Override
    public String toString() {
        return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: "
                + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time
                + " Capacity: " + capacity + " Price: " + price;
    }
}