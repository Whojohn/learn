package udf;

import org.apache.flink.table.annotation.DataTypeHint;
import org.roaringbitmap.RoaringBitmap;

public class BitmapWrapper {


    @DataTypeHint("RAW")
    public RoaringBitmap roaringBitmap;


    public BitmapWrapper() {
        this.roaringBitmap = new RoaringBitmap();
    }


    public long getLongCardinality() {
        return this.roaringBitmap.getLongCardinality();
    }

    public void add(int value) {
        this.roaringBitmap.add(value);
    }


    public void merge(Iterable<BitmapWrapper> it) {
        for (BitmapWrapper other : it) {
            if (other != null) {
                this.roaringBitmap.or(other.roaringBitmap);
            }
        }
    }

    public int last() {
        return this.roaringBitmap.last();
    }

    public boolean isEmpty() {
        return this.roaringBitmap.isEmpty();
    }
}
