public class Page 
{
  public int id;
  public int physical;
  public byte R;
  public byte M;
  public int inMemTime;
  public int lastTouchTime;
  public long high;
  public long low;
  public long result;
  public long seg1_v;
  public long seg2_v;


  public Page( int id, int physical, byte R, byte M, int inMemTime, int lastTouchTime, long high, long low, long result,long seg1,int seg2) 
  {
    this.id = id;
    this.physical = physical;
    this.R = R;
    this.M = M;
    this.inMemTime = inMemTime;
    this.lastTouchTime = lastTouchTime;
    this.high = high;
    this.low = low;
    this.result = result;
    this.seg1_v = seg1;
    this.seg2_v = seg2;
  } 	
  /*
  //setter/getter
    //------------Segmentos-------------------
    public void setSegmento(int _seg1,int _seg2){
        this.seg1_v = _seg1;
        this.seg2_v = _seg2;
    }
    public int getseg1(){
        return seg1_v;
    }

    public int getseg2(){
      return seg2_v;
    }
    */

}
