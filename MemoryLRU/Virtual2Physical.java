import java.util.Vector;

public class Virtual2Physical 
{
  public static int pageNum ( long memaddr , int numpages , long block) 
  {
    int i = 0;
    long high = 0;
    long low = 0;
    long result = 0;
    
    for (i = 0; i <= numpages; i++) 
    {
      low = block * i;
      high = block * ( i + 1 ); 
      result = i;
      //    0 <= 0000  &&  0000 < 4095
      if ( low <= memaddr && memaddr < high ) 
      {
        return i;
      }
    } 
    return -1;
  }
}
