package flightapp;

public class Itinerary implements Comparable<Itinerary> {
    int id;
    Flight f1;
    Flight f2;
    int price;
    boolean layover;
    int totalTime;
    public Itinerary(Flight f) {
        this.f1 = f;
        this.layover = false;
        this.totalTime = f.time;
        this.price = f.price;
        this.id = 0;
    }

    public Itinerary(Flight f1, Flight f2) {
        this.f1 = f1;
        this.f2 = f2;
        this.layover = true;
        this.totalTime = f1.time + f2.time;
        this.price = f1.price + f2.price;
        this.id = 0;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int hashCode() {
        String res = "" + f1.fid;
        if (layover) {
            res += "#" + f2.fid;
        }
        return res.hashCode();
    }

    public int compareTo(Itinerary other) {
        if (this.totalTime == other.totalTime) {
            if (this.f1.fid == other.f1.fid) {
                return this.f2.fid - other.f2.fid;
            }
            return this.f1.fid - other.f1.fid;
        }
        return this.totalTime - other.totalTime;
    }
}