import java.lang.Thread;
import java.io.*;
import java.util.*;
//import Page;

public class Kernel extends Thread
{
  // The number of virtual pages must be fixed at 63 due to
  // dependencies in the GUI
  private static int virtPageNum = 63;

  private String output = null;
  private static final String lineSeparator = 
    System.getProperty("line.separator");
  private String command_file;
  private String config_file;
  private ControlPanel controlPanel ;
  private Vector memVector = new Vector();
  private Vector instructVector = new Vector();
  private String status;
  private boolean doStdoutLog = false;
  private boolean doFileLog = false;
  public int runs;
  public int runcycles;
  public long block = (int) Math.pow(2,12);
  public static byte addressradix = 10;
  public int index;
  //----------------EXTRA-------------------------
  long seg1_value = 0;
  long seg2_value = 0;
  long aux_seg1 = 0;
  long aux_seg2 = 0;
  public Vector<Long> key = new Vector(63);//seg1
  public Vector<Long> value = new Vector(63);//seg2
  int cont_step = 0;
  //-----------------------------------------------

  public void asignacion(Vector<Long> stack){
    for(int i=0;i<63;i++){
      stack.addElement((long)0); 
    }
  }

  public int numPagina(long num){
    long begin = 0,end = 16383;
    int pagina = 0;
    boolean pass = false;

    while(!pass){
      if(num >= begin && num <= end){
        pass = true;
      }else{
        begin = end+1;
        end = begin + 16383;
        pagina++; 
      }
    }
    return pagina;
  }

  public boolean parametro(long num,long pag){
    //4096
    //0
    long begin = 0,end = 16383;
    int aux_pag = 0;
    while(true){
      if(aux_pag == pag){
        if(num>=begin && num<=end){
          return true;
        }
        break;
      }else{
        begin = end+1;
        end = begin + 16383;
        aux_pag++;
      }
    }

    return false;
  }

  public int numSegmento(long num){
    //      0000   -   0fff
    //      1000   -   1fff
    //      4096       8191
  long begin = 0,end = 4095;
  int different = 4095;
  int num_seg = 1;
  boolean pass = false;

  while(!pass){
      if(num >= begin && num <= end){
        pass = true;
      }else{
        begin = end+1;
        end = begin+different;
        num_seg++;
      }
    }

    if(num_seg > 4){
      if((num_seg%4)==0){
        num_seg = 4;
      }else{
        num_seg = num_seg%4;
      }
    }

    return num_seg;
  }

  public boolean rIsFull(Vector stack){
    for(int i=0;i<=31;i++){
      Page page = (Page) stack.elementAt(i);
        if(page.R != 1){
          return false;
        }
    }
    return true;
  }
  //-------------------------------------------------
  //----------------FUNCIONS 1----------------------------
  public void init( String commands , String config )  
  {
    asignacion(key);
    asignacion(value);

    
    File f = new File( commands );
    command_file = commands;
    config_file = config;
    String line;
    String tmp = null;
    String command = "";
  
    byte R = 0;
    byte M = 0;
    int i = 0;
    int j = 0;
    int id = 0;
    int physical = 0;
    int physical_count = 0;
    int inMemTime = 0;
    int lastTouchTime = 0;
    int map_count = 0;
    double power = 14;
    long high = 0;
    long low = 0;
    long addr = 0;
    //----------------
    long result = 0;
    //----------------
    long address_limit = (block * virtPageNum+1)-1;
  
    if ( config != null )
    {
      f = new File ( config );
      try 
      {
        DataInputStream in = new DataInputStream(new FileInputStream(f));
        while ((line = in.readLine()) != null) 
        {
          if (line.startsWith("numpages")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) 
            {
              tmp = st.nextToken();
              virtPageNum = Common.s2i(st.nextToken()) - 1;
              if ( virtPageNum < 2 || virtPageNum > 63 )
              {
                System.out.println("MemoryManagement: numpages out of bounds.");
                System.exit(-1);
              }
              address_limit = (block * virtPageNum+1)-1;
            }
          }
        }
        in.close();
      } catch (IOException e) { /* Handle exceptions */ }
      for (i = 0; i <= virtPageNum; i++) 
      {
        high = (block * (i + 1))-1;
        low = block * i;
        result = Long.parseLong(Integer.toString(i),16);
        //seg1_v = 0;
        memVector.addElement(new Page(i, -1, R, M, 0, 0, high, low, result,0,0));
      }
      try 
      {
        DataInputStream in = new DataInputStream(new FileInputStream(f));
        while ((line = in.readLine()) != null) 

        {
          if (line.startsWith("memset")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            st.nextToken();
            while (st.hasMoreTokens()) 
            { 
              id = Common.s2i(st.nextToken());
              tmp = st.nextToken();
              if (tmp.startsWith("x")) 
              {
                physical = -1;
              } 
              else 
              {
                physical = Common.s2i(tmp);
              }
              if ((0 > id || id > virtPageNum) || (-1 > physical || physical > ((virtPageNum - 1) / 2)))
              {
                System.out.println("MemoryManagement: Invalid page value in " + config);
                System.exit(-1);
              }
              R = Common.s2b(st.nextToken());
              if (R < 0 || R > 1)
              {
                System.out.println("MemoryManagement: Invalid R value in " + config);
                System.exit(-1);
              }
              M = Common.s2b(st.nextToken());
              if (M < 0 || M > 1)
              {
                 System.out.println("MemoryManagement: Invalid M value in " + config);
                 System.exit(-1);
              }
              inMemTime = Common.s2i(st.nextToken());
              if (inMemTime < 0)
              {
                System.out.println("MemoryManagement: Invalid inMemTime in " + config);
                System.exit(-1);
              }
              lastTouchTime = Common.s2i(st.nextToken());
              if (lastTouchTime < 0)
              {
                System.out.println("MemoryManagement: Invalid lastTouchTime in " + config);
                System.exit(-1);
              }
              Page page = (Page) memVector.elementAt(id);
              page.physical = physical;
              page.R = R;
              page.M = M;
              page.inMemTime = inMemTime;
              page.lastTouchTime = lastTouchTime;

            }
          }
          if (line.startsWith("enable_logging")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) 
            {
              if ( st.nextToken().startsWith( "true" ) )
              {
                doStdoutLog = true;
              }              
            }
          }
          if (line.startsWith("log_file")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) 
            {
              tmp = st.nextToken();
            }
            if ( tmp.startsWith( "log_file" ) )
            {
              doFileLog = false;
              output = "tracefile";
            }              
            else
            {
              doFileLog = true;
              doStdoutLog = false;
              output = tmp;
            }
          }
          if (line.startsWith("pagesize")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) 
            {
              tmp = st.nextToken();
              tmp = st.nextToken();
              if ( tmp.startsWith( "power" ) )
              {
                power = (double) Integer.parseInt(st.nextToken());
                block = (int) Math.pow(2,power);
              }
              else
              {
                block = Long.parseLong(tmp,10);             
              }
              address_limit = (block * virtPageNum+1)-1;
            }
            if ( block < 64 || block > Math.pow(2,26))
            {
              System.out.println("MemoryManagement: pagesize is out of bounds");
              System.exit(-1);
            }
            for (i = 0; i <= virtPageNum; i++) 
            {
              Page page = (Page) memVector.elementAt(i);
              page.high = (block * (i + 1))-1;
              page.low = block * i;
              //page.result = i;

            }
          }
          if (line.startsWith("addressradix")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) 
            {
              tmp = st.nextToken();
              tmp = st.nextToken();
              addressradix = Byte.parseByte(tmp);
              if ( addressradix < 0 || addressradix > 20 )
              {
                System.out.println("MemoryManagement: addressradix out of bounds.");
                System.exit(-1);
              }
            }
          }
        }
        in.close();
      } catch (IOException e) { /* Handle exceptions */ }
    }
    f = new File ( commands );
    try 
    {
      DataInputStream in = new DataInputStream(new FileInputStream(f));
      while ((line = in.readLine()) != null) //bucle
      {
        if (line.startsWith("READ") || line.startsWith("WRITE")) 
        {
          cont_step++;//<---------------------------------------------
          if (line.startsWith("READ")) 
          {
            command = "READ";
          }
          if (line.startsWith("WRITE")) 
          {
            command = "WRITE";
          }
          StringTokenizer st = new StringTokenizer(line);
          // read hex 1000
          tmp = st.nextToken();//read
          tmp = st.nextToken();//hex

          if (tmp.startsWith("random")) 
          {
            instructVector.addElement(new Instruction(command,Common.randomLong( address_limit )));
          } 
          else 
          { 
            if ( tmp.startsWith( "bin" ) )
            {
              addr = Long.parseLong(st.nextToken(),2);             
            }
            else if ( tmp.startsWith( "oct" ) )
            {
              addr = Long.parseLong(st.nextToken(),8);
            }
            else if ( tmp.startsWith( "hex" ) )
            {
              addr = Long.parseLong(st.nextToken(),16);

              aux_seg1 = Long.parseLong(st.nextToken(),16);
              //4096
              aux_seg2 = Long.parseLong(st.nextToken(),16);
              //12287
              seg1_value = numSegmento(aux_seg1);
              // 2
              seg2_value = numSegmento(aux_seg2);
              // 3

              int num_pagina = numPagina(addr);

              System.out.println("p: "+num_pagina);

              if(parametro(aux_seg1,num_pagina)){
                key.insertElementAt(seg1_value, num_pagina);  
              }else{
                key.insertElementAt((long)-1, num_pagina); 
              }
              
              if(parametro(aux_seg2,num_pagina)){
                value.insertElementAt(seg2_value, num_pagina);  
              }else{
                value.insertElementAt((long)-1, num_pagina);                
              }

              System.out.println("S1 : "+seg1_value);
              System.out.println("S2 : "+seg2_value);

            }
            else
            {
              addr = Long.parseLong(tmp);
            }
            if (0 > addr || addr > address_limit)
            {
              System.out.println("MemoryManagement: " + addr + ", Address out of range in " + commands);
              System.exit(-1);
            }
            instructVector.addElement(new Instruction(command,addr));
          }
        }
      }

      in.close();
    } catch (IOException e) { /* Handle exceptions */ }
    runcycles = instructVector.size();
    if ( runcycles < 1 )
    {
      System.out.println("MemoryManagement: no instructions present for execution.");
      System.exit(-1);
    }
    if ( doFileLog )
    {
      File trace = new File(output);
      trace.delete();
    }
    runs = 0;
    for (i = 0; i < virtPageNum; i++) 
    {
      Page page = (Page) memVector.elementAt(i);
      if ( page.physical != -1 )
      {
        map_count++;
      }
      for (j = 0; j < virtPageNum; j++) 
      {
        Page tmp_page = (Page) memVector.elementAt(j);
        if (tmp_page.physical == page.physical && page.physical >= 0)
        {
          physical_count++;
        }
      }
      if (physical_count > 1)
      {
        System.out.println("MemoryManagement: Duplicate physical page's in " + config);
        System.exit(-1);
      }
      physical_count = 0;
    }
    if ( map_count < ( virtPageNum +1 ) / 2 )
    {
      for (i = 0; i < virtPageNum; i++) 
      {
        Page page = (Page) memVector.elementAt(i);
        if ( page.physical == -1 && map_count < ( virtPageNum + 1 ) / 2 )
        {
          page.physical = i;
          map_count++;
        }
      }
    }
    for (i = 0; i < virtPageNum; i++) 
    {
      Page page = (Page) memVector.elementAt(i);
      if (page.physical == -1) 
      {
        controlPanel.removePhysicalPage( i );
      } 
      else
      {
        controlPanel.addPhysicalPage( i , page.physical );
      }
    }
    for (i = 0; i < instructVector.size(); i++) 
    {
      high = block * virtPageNum;
      Instruction instruct = ( Instruction ) instructVector.elementAt( i );
      if ( instruct.addr < 0 || instruct.addr > high )
      {
        System.out.println("MemoryManagement: Instruction (" + instruct.inst + " " + instruct.addr + ") out of bounds.");
        System.exit(-1);
      }
    }
  } 

  //----------------FUNCIONS 2----------------------------


  public void setControlPanel(ControlPanel newControlPanel) 
  {
    controlPanel = newControlPanel ;
  }

  //----------------FUNCIONS 3----------------------------


  public void getPage(int pageNum) 
  {
    Page page = ( Page ) memVector.elementAt( pageNum );
    controlPanel.paintPage( page );
  }

  //----------------FUNCIONS 4----------------------------

  private void printLogFile(String message)
  {
    String line;
    String temp = "";

    File trace = new File(output);
    if (trace.exists()) 
    {
      try 
      {
        DataInputStream in = new DataInputStream( new FileInputStream( output ) );
        while ((line = in.readLine()) != null) {
          temp = temp + line + lineSeparator;
        }
        in.close();
      }
      catch ( IOException e ) 
      {
        /* Do nothing */
      }
    }
    try 
    {
      PrintStream out = new PrintStream( new FileOutputStream( output ) );
      out.print( temp );
      out.print( message );
      out.close();
    } 
    catch (IOException e) 
    {
      /* Do nothing */ 
    }
  }
//------------------FUNCION 5-------------------
  public void run()
  {
    step();
    while (runs != runcycles) 
    {
      try 
      {
        Thread.sleep(2000);
      } 
      catch(InterruptedException e) 
      {  
        /* Do nothing */ 
      }
      step();
    }  
  }

  //----------------FUNCIONS 6----------------------------

  public void step()
  {
    int i = 0;

    Instruction instruct = ( Instruction ) instructVector.elementAt( runs );
    controlPanel.instructionValueLabel.setText( instruct.inst );
    controlPanel.addressValueLabel.setText( Long.toString( instruct.addr , addressradix ) );
    getPage( Virtual2Physical.pageNum( instruct.addr , virtPageNum , block ) );
    if ( controlPanel.pageFaultValueLabel.getText() == "YES" ) 
    {
      controlPanel.pageFaultValueLabel.setText( "NO" );
    }
    if ( instruct.inst.startsWith( "READ" ) ) 
    {
      index = Virtual2Physical.pageNum( instruct.addr , virtPageNum , block );
      Page page = ( Page ) memVector.elementAt(index);
      if ( page.physical == -1 ) 
      {
        if ( doFileLog )
        {
          printLogFile( "READ " + Long.toString(instruct.addr , addressradix) + " ... page fault" );
        }
        if ( doStdoutLog )
        {
          System.out.println( "READ " + Long.toString(instruct.addr , addressradix) + " ... page fault" );
        }
        PageFault.replacePage( memVector , virtPageNum ,index , controlPanel );
        controlPanel.pageFaultValueLabel.setText( "YES" );
      } 
      else 
      {
        page.R = 1;
        page.lastTouchTime = 0; 
        //--------------------
        page.seg2_v = key.elementAt(index);
        page.seg1_v = value.elementAt(index);
        //--------------------  
        if ( doFileLog )
        {
          printLogFile( "READ " + Long.toString( instruct.addr , addressradix ) + " ... okay" );
        }
        if ( doStdoutLog )
        {
          System.out.println( "READ " + Long.toString( instruct.addr , addressradix ) + " ... okay" );
        }
      }
    }
    if ( instruct.inst.startsWith( "WRITE" ) ) 
    {
      index = Virtual2Physical.pageNum( instruct.addr , virtPageNum , block );
      Page page = ( Page ) memVector.elementAt(index);
      if ( page.physical == -1 ) 
      {
        if ( doFileLog )
        {
          printLogFile( "WRITE " + Long.toString(instruct.addr , addressradix) + " ... page fault" );
        }
        if ( doStdoutLog )
        {
           System.out.println( "WRITE " + Long.toString(instruct.addr , addressradix) + " ... page fault" );
        }
        PageFault.replacePage( memVector , virtPageNum , Virtual2Physical.pageNum( instruct.addr , virtPageNum , block ) , controlPanel );          
        controlPanel.pageFaultValueLabel.setText( "YES" );
      } 
      else 
      {
        page.M = 1;
        page.lastTouchTime = 0;
        //--------------------
        page.seg2_v = key.elementAt(index);
        page.seg1_v = value.elementAt(index);
        //--------------------  
        if ( doFileLog )
        {
          printLogFile( "WRITE " + Long.toString(instruct.addr , addressradix) + " ... okay" );
        }
        if ( doStdoutLog )
        {
          System.out.println( "WRITE " + Long.toString(instruct.addr , addressradix) + " ... okay" );
        }
      }
    }

    cont_step--;//<-------------------------------------
    for ( i = 0; i < virtPageNum; i++ ) 
    {
      Page page = ( Page ) memVector.elementAt( i );
      if ( page.R == 1 && page.lastTouchTime == 310 ) // <-- 31 paginas
      {
        page.R = 0;
      }
      if ( page.physical != -1 ) 
      {
        page.inMemTime = page.inMemTime + 10;
        page.lastTouchTime = page.lastTouchTime + 10;
      }
    }
    runs++;
    controlPanel.timeValueLabel.setText( Integer.toString( runs*10 ) + " (ns)" );
    
    //------------------------
    if(cont_step == 0){
      if(rIsFull(memVector) == true){//JORGE
        for(int k = 0;k <= 31; k++){
          Page page = ( Page ) memVector.elementAt(k);
          if(page.lastTouchTime == page.inMemTime){
            PageFault.replacePageLRU(page.physical,memVector,controlPanel);
            System.out.println("Numero de pagina en intercambio: "+page.physical);
          }
        }
      }else{//JOHANA
        boolean flag = true;
        while(flag){
          Page page = ( Page ) memVector.elementAt((int)(Math.random()*32));//0 - 31
          if(page.R != 1){
            PageFault.replacePageLRU(page.physical,memVector,controlPanel);
            System.out.println("Numero de pagina en intercambio: "+page.physical);
            flag = false;
          }
        }
      }
    }


  }

  

  //----------------FUNCIONS 6----------------------------

  public void reset() {
    memVector.removeAllElements();
    instructVector.removeAllElements();
    controlPanel.statusValueLabel.setText( "STOP" ) ;
    controlPanel.timeValueLabel.setText( "0" ) ;
    controlPanel.instructionValueLabel.setText( "NONE" ) ;
    controlPanel.addressValueLabel.setText( "NULL" ) ;
    controlPanel.pageFaultValueLabel.setText( "NO" ) ;
    controlPanel.virtualPageValueLabel.setText( "x" ) ;
    controlPanel.physicalPageValueLabel.setText( "0" ) ;
    controlPanel.RValueLabel.setText( "0" ) ;
    controlPanel.MValueLabel.setText( "0" ) ;
    controlPanel.inMemTimeValueLabel.setText( "0" ) ;
    controlPanel.lastTouchTimeValueLabel.setText( "0" ) ;
    controlPanel.lowValueLabel.setText( "0" ) ;
    controlPanel.highValueLabel.setText( "0" ) ;
    controlPanel.resultValueLabel.setText( "0" ) ;
    controlPanel.Seg1ValueLabel.setText( "0" ) ;
    controlPanel.Seg2ValueLabel.setText( "0" ) ;

    init( command_file , config_file );
  }
}
