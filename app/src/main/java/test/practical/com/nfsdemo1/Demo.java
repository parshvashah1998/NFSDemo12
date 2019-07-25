package test.practical.com.nfsdemo1;

import android.app.Activity;
import android.app.PendingIntent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import FWPubLib.pl_javacall;
import Lib.FWReader.S3.function_S3;
import Lib.FWReader.S8.function_S8;
import Lib.HNCOS.HNCOS;


public class Demo extends Activity {

    public static final char UI_UPDATE_BTN_AUTO = 1;
    public static final char UI_UPDATE_BTN_MANUAL_DISABLE = 2;
    public static final char UI_UPDATE_BTN_MANUAL_ENABLE = 3;
    public static final char UI_UPDATE_MSG_TEXT_APPEND = 4;
    public static final char UI_UPDATE_MSG_TEXT_SET = 5;
    public static final char PT_USB = 2;
    public static final char PT_SERIAL = 1;
    public static final char DEV_S3 = 0;
    public static final char DEV_S8 = 1;
    public static final char DEV_E7 = 2;
    public static final char CARD_4442 = 0x01;
    public static final char CARD_4428 = 0x02;
    public static final char CARD_24Cxx = 0x03;
    public static final char CARD_102 = 0x04;
    public static final char CARD_TC_CPU = 0x15;
    public static final char CARD_M1 = 0x20;
    public static final char CARD_ULTRILIGHT = 0x21;
    public static final char CARD_DESFIRE = 0x22;
    public static final char CARD_CTL_CPU = 0x23;
    public static final char CARD_ICODE2 = 0x24;
    public static final char CARD_SECONDID = 0x25;
    public static final char CARD_PSAM1 = 0xf1;
    public static final char FUNC_KB = 0xE1;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final char[] FileType_EF = {'0', '2'};
    private static final char[] IDBinEF_DF01 = {'0', '0', '0', '5'};
    static int fileSize_standard = (int) 0x20;
    static byte recordSize = (byte) 0x0A;
    public char gl_autoRun = 0, gl_autoRunning = 0;
    public char gl_singleTestInAutoRunning = 0;
    public Handler m_Handler;
    public Message message = new Message();
    public String gl_msg, gl_autoBtnText;
    int lenSingleChar = 2, lenHex;
    char[] pCharHex = new char[255];
    char[] pCharSingle = new char[255];
    Object mByteReceivedBackSemaphore = new Object();
    AutoTestThread mAutoThread = null;
    int struct_deviceType = DEV_S8;
    int struct_portType = PT_USB;
    int struct_cardType = CARD_M1;
    int hdev = -1;
    private RadioGroup grp_serialType;
    private RadioGroup grp_devType;
    private RadioGroup grp_cardType;
    private Button m_btn, m_btn_autoTest, m_btn_clean;
    private EditText m_text;
    private EditText m_text_devPath;
    private EditText m_text_baud;
    private EditText m_text_pid;
    private EditText m_text_vid;
    private UsbDeviceConnection connection;
    private UsbEndpoint inEndpoint = null;
    private UsbEndpoint outEndpoint = null;
    private PendingIntent pendingIntent;
    //private int VendorID;//s8/s3: 0x471(1137), E7:0x0483(1155)
    //private int ProductID;//s8/s3: 0xa112(41234), E7:0x5750(22352)
    private function_S3 call_s3;
    private function_S8 call_s8;
    private HNCOS obj_hncos;
    private pl_javacall publib;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_test_main);


        m_btn = (Button) findViewById(R.id.button1);
        m_btn_autoTest = (Button) findViewById(R.id.btn_autoTest);
        m_btn_clean = (Button) findViewById(R.id.btn_clean);
        m_text = (EditText) findViewById(R.id.editText1);
        m_text_devPath = (EditText) findViewById(R.id.edt_devPath);
        m_text_baud = (EditText) findViewById(R.id.edt_baud);
        //m_text_pid = (EditText)findViewById(R.id.edt_pid);
        //m_text_vid = (EditText)findViewById(R.id.edt_vid);
        grp_serialType = (RadioGroup) findViewById(R.id.radioGroup_serialType);
        grp_devType = (RadioGroup) findViewById(R.id.radioGroup_device);
        grp_cardType = (RadioGroup) findViewById(R.id.radioGroup_cardType);


        call_s3 = new function_S3(this);
        call_s8 = new function_S8(this);
        publib = new pl_javacall();

        UpdateUIForPortType(false);

        call_s3.SetTransPara(0x20, 1137, 41234);
        call_s8.SetTransPara(0x20, 1137, 41234);

        //call_s3.configLogFile(((String)("/udisk/dlllog.txt")).toCharArray());
        call_s3.configLogFile(((String) ("/sdcard/dlllog.txt")).toCharArray());


        m_Handler = new Handler() {
            public void handleMessage(Message msg) {
                int textSize;

                switch (msg.what) {
                    case UI_UPDATE_MSG_TEXT_APPEND:
                    case UI_UPDATE_MSG_TEXT_SET:
                        m_text.setText(gl_msg);
                        break;
                    case UI_UPDATE_BTN_AUTO:
                        m_btn_autoTest.setText(gl_autoBtnText);
                        break;
                    case UI_UPDATE_BTN_MANUAL_DISABLE:
                        m_btn.setEnabled(false);
                        break;
                    case UI_UPDATE_BTN_MANUAL_ENABLE:
                        m_btn.setEnabled(true);
                        break;
                    default:
                        break;
                }

                textSize = m_text.getText().length();
                m_text.setSelection(textSize);
            }
        };


        m_btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                DoOneTest();
            }
        });

        m_btn_autoTest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                if (0 == gl_autoRun) {
                    gl_autoRunning = 1;

                    mAutoThread = new AutoTestThread();
                    mAutoThread.start();
                } else {
                    gl_autoRunning = 0;
                }
            }
        });

        m_btn_clean.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                m_text.setText("");
                gl_msg = "";
            }
        });

        grp_serialType.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup arg0, int arg1) {
                // TODO Auto-generated method stub
                int radioButtonId = arg0.getCheckedRadioButtonId();

                if (radioButtonId == R.id.radioSerail) {
                    struct_portType = PT_SERIAL;
                    UpdateUIForPortType(true);
                } else if (radioButtonId == R.id.radioUSB) {
                    struct_portType = PT_USB;
                    UpdateUIForPortType(false);
                }

            }
        });
        grp_devType.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup arg0, int arg1) {
                // TODO Auto-generated method stub
                int radioButtonId = arg0.getCheckedRadioButtonId();

                if (radioButtonId == R.id.radioS8) {
                    struct_deviceType = DEV_S8;
                    call_s8.SetTransPara(0x20, 1137, 41234);
                } else if (radioButtonId == R.id.radioS3) {
                    struct_deviceType = DEV_S3;
                    call_s3.SetTransPara(0x20, 1137, 41234);
                } else if (radioButtonId == R.id.radioE7) {
                    struct_deviceType = DEV_E7;
                    call_s8.SetTransPara(0x40, 1155, 22352);
                }
            }
        });

        grp_cardType.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // TODO Auto-generated method stub

                if (checkedId == R.id.radio_card_M1)
                    struct_cardType = CARD_M1;
                else if (checkedId == R.id.radio_card_UltraLight)
                    struct_cardType = CARD_ULTRILIGHT;
                else if (checkedId == R.id.radio_card_Desfire)
                    struct_cardType = CARD_DESFIRE;
                else if (checkedId == R.id.radio_card_CTL_CPU)
                    struct_cardType = CARD_CTL_CPU;
                else if (checkedId == R.id.radio_card_ICODE2)
                    struct_cardType = CARD_ICODE2;
                else if (checkedId == R.id.radio_card_SecondID)
                    struct_cardType = CARD_SECONDID;
                else if (checkedId == R.id.radio_card_4442)
                    struct_cardType = CARD_4442;
                else if (checkedId == R.id.radio_card_4428)
                    struct_cardType = CARD_4428;
                else if (checkedId == R.id.radio_card_24Cxx)
                    struct_cardType = CARD_24Cxx;
                else if (checkedId == R.id.radio_card_102)
                    struct_cardType = CARD_102;
                else if (checkedId == R.id.radio_card_CT_CPU)
                    struct_cardType = CARD_TC_CPU;
                else if (checkedId == R.id.radio_card_PSAM1)
                    struct_cardType = CARD_PSAM1;
                else if (checkedId == R.id.radio_KeyBoard)
                    struct_cardType = FUNC_KB;

            }
        });

    }

    public void UpdateUIForPortType(boolean benable) {
        m_text_devPath.setFocusable(benable);
        m_text_baud.setFocusable(benable);
        m_text_devPath.setEnabled(benable);
        m_text_baud.setEnabled(benable);
        m_text_devPath.setFocusableInTouchMode(benable);
        m_text_baud.setFocusableInTouchMode(benable);

    }

    private void SendUIMessage(char toWhat, String text) {
        switch (toWhat) {
            case UI_UPDATE_MSG_TEXT_APPEND:
                gl_msg += text + "\n";
                break;
            case UI_UPDATE_MSG_TEXT_SET:
                gl_msg = text + "\n";
                break;
            case UI_UPDATE_BTN_AUTO:
                gl_autoBtnText = text;
                break;
        }
        m_Handler.obtainMessage(toWhat).sendToTarget();
    }

    public void ClearMsg() {
        gl_msg = "";
    }

    public int DoOneTest() {
        String devPath;
        int baud;
        int testItems = 0;

        ClearMsg();

        devPath = m_text_devPath.getText().toString();
        baud = Integer.parseInt(m_text_baud.getText().toString());

        if ((struct_deviceType == DEV_S8) || (struct_deviceType == DEV_E7)) {
            if (struct_cardType == CARD_M1)
                TestM1(struct_portType, devPath, baud);
            else if (struct_cardType == CARD_ULTRILIGHT)
                TestUltralight(struct_portType, devPath, baud);
            else if (struct_cardType == CARD_DESFIRE)
                TestDesfire(struct_portType, devPath, baud);
            else if (struct_cardType == CARD_CTL_CPU)
                TestCTLcpu(struct_portType, devPath, baud);
            else if (struct_cardType == CARD_ICODE2)
                TestICODE2(struct_portType, devPath, baud);
            else if (struct_cardType == CARD_SECONDID)
                TestSecondIDcard(struct_portType, devPath, baud);
            else if (struct_cardType == CARD_4442)
                Test_S8_4442(struct_portType, devPath, baud);
            else if (struct_cardType == CARD_PSAM1)
                Test_S8_PSAM(struct_portType, devPath, baud);
            else if (struct_cardType == FUNC_KB)
                TestFuncKB(struct_portType, devPath, baud);
        } else if (struct_deviceType == DEV_S3) {
            if (struct_cardType == CARD_4442)
                Test_S3_4442(struct_portType, devPath, baud);
            else if (struct_cardType == CARD_4428)
                Test_S3_4428(struct_portType, devPath, baud);
            else if (struct_cardType == CARD_24Cxx)
                Test_S3_24Cxx(struct_portType, devPath, baud);
            else if (struct_cardType == CARD_102)
                Test_S3_102(struct_portType, devPath, baud);
            else if (struct_cardType == CARD_TC_CPU)
                Test_S3_Cpu(struct_portType, devPath, baud);
            else if (struct_cardType == CARD_PSAM1)
                Test_S3_PSAM(struct_portType, devPath, baud);

        }


        return 0;
    }

    public void TestM1(int portType, String path, int baudrate) {
        // TODO Auto-generated method stub
        int result = 0, hdev = 1;
        char[] pModVer = new char[512];
        char[] pSnrM1 = new char[255];
        char[] pCharHex = new char[255];
        char[] pCharSingle = new char[255];
        int lenSingleChar = 2, lenHex;
        short tblk = 24;
        short val_blk = 25;
        int[] pCurVal = new int[1];
        short tSec = (short) (tblk / 4);
        short keymode = 0;
        char[] defKey = {0xff, 0xff, 0xff, 0xff, 0xff, 0xff};
        char[] newKey = {0xff, 0xff, 0xff, 0xff, 0xff, 0xff};
        char[] strNewkey = new char[255];
        char[] tWrite = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0xd, 0xe, 0xf};
        char[] strHexWrite = new char[1024];
        char[] strHexRead = new char[1024];
        char[] strKeyb = ("ffffffffffff").toCharArray();
        char[] strCtrlW = ("ff078069").toCharArray();


        lenHex = 2 * lenSingleChar;
        pCharHex[0] = 0x33;
        pCharHex[1] = 0x31;
        pCharHex[2] = 0x33;
        pCharHex[3] = 0x32;

        call_s8.a_hex(pCharSingle, pCharHex, lenSingleChar);
        pCharSingle[lenSingleChar] = 0;
        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " a_hex:" + String.valueOf(pCharSingle));

        call_s8.hex_a(pCharHex, pCharSingle, lenHex);
        pCharHex[lenHex] = 0;
        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " hex_a:" + String.valueOf(pCharHex));

        if (portType == PT_USB) hdev = call_s8.fw_init_ex(2, null, 0);
        else hdev = call_s8.fw_init_ex(1, path.toCharArray(), baudrate);
        if (hdev != -1) {

            //call_s8.fw_beep(hdev, 5);

            //try to get module version
            result = call_s8.fw_getver(hdev, pModVer);
            if (0 == result) {

                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "-");
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "Module Version: " + String.valueOf(pModVer));


                call_s8.fw_load_key(hdev, keymode, tSec, defKey);

                result = call_s8.fw_card_str(hdev, (short) 1, pSnrM1);
                if (0 == result) {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_card:ok ");
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, String.valueOf(pSnrM1));

                    // authen
                    result = call_s8.fw_authentication(hdev, keymode, tSec);

                    if (0 == result) {
                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_authen:ok ");

                        //write
                        //result = call.fw_write(hdev, tblk, tWrite);


                        call_s8.hex_a(strHexWrite, tWrite, 2 * (tWrite.length));
                        result = call_s8.fw_write_hex(hdev, tblk, strHexWrite);


                        if (0 == result) {
                            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_write:ok ");

                            //read
                            //result = call.fw_read(hdev, tblk, tRead);


                            result = call_s8.fw_read_hex(hdev, tblk, strHexRead);

                            if (0 == result) {
                                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_read:ok ");

                                //	for(i=0;i<16;i++)
                                //	{
                                //		SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND," "+Integer.toHexString(tRead[i]));
                                //	}

                                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " " + String.valueOf(strHexRead));

                                //value test
                                result = call_s8.fw_initval(hdev, val_blk, 1000);
                                if (0 == result) {
                                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " _initval:ok");

                                    result = call_s8.fw_increment(hdev, val_blk, 200);
                                    if (0 == result) {
                                        call_s8.fw_transfer(hdev, val_blk);//make increment valid
                                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " _increment:ok");

                                        result = call_s8.fw_decrement(hdev, val_blk, 100);
                                        if (0 == result) {
                                            call_s8.fw_transfer(hdev, val_blk);//make decrement valid
                                            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " _decrement:ok");

                                            result = call_s8.fw_readval(hdev, val_blk, pCurVal);
                                            if (0 == result) {
                                                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " _readval ok:" + pCurVal[0]);
                                            } else
                                                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " _readval error");
                                        } else
                                            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " _decrement:error");
                                    } else
                                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " _increment:error");

                                } else SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " _initval:error");


                                //result = call.changeKey(hdev, tSec, newKey, ctrlw, keyb);

                                call_s8.hex_a(strNewkey, newKey, 2 * (newKey.length));
                                result = call_s8.fw_changeKey_hex(hdev, tSec, strNewkey, strCtrlW, strKeyb);


                                if (0 == result) {
                                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " _changekey:ok");

                                    result = call_s8.fw_halt(hdev);
                                    if (0 == result) {
                                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " _halt:ok");
                                    } else {
                                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " _halt:error");
                                    }
                                } else {
                                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " _changekey:error");
                                }

                            } else {
                                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_read:error ");
                            }
                        } else {
                            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_write:error ");
                        }

                    } else {
                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_athen:error ");

                    }

                }
            }

            call_s8.fw_exit(hdev);
        } else {
            //Log.e("readerlog", "Link reader error");
        }

    }

    public void TestUltralight(int portType, String path, int baudrate) {
        // TODO Auto-generated method stub
        int result = 0, hdev = 1;
        char[] pModVer = new char[512];
        char[] pSnrM1 = new char[255];
        char[] pCharSingle = new char[255];
        int i;
        short tPage = 4;
        char[] tWrite = {0x01, 0x02, 0x03, 0x04};
        char[] tRead = new char[512];
        if (portType == PT_USB) hdev = call_s8.fw_init_ex(2, null, 0);
        else hdev = call_s8.fw_init_ex(1, path.toCharArray(), baudrate);
        if (hdev != -1) {

            call_s8.fw_beep(hdev, 5);

            //try to get module version
            result = call_s8.fw_getver(hdev, pModVer);
            if (0 == result) {
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "-");
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "Module Version: " + String.valueOf(pModVer));

                result = call_s8.fw_card_str(hdev, (short) 1, pSnrM1);
                if (0 == result) {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_card:ok ");
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, String.valueOf(pSnrM1));

                    // read page 0
                    result = call_s8.fw_read_ultralt(hdev, (short) 0, pCharSingle);

                    if (0 == result) {
                        //write
                        result = call_s8.fw_write_ultralt(hdev, tPage, tWrite);

                        if (0 == result) {
                            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_write:ok ");

                            //read
                            result = call_s8.fw_read_ultralt(hdev, tPage, tRead);

                            if (0 == result) {
                                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_read:ok ");

                                for (i = 0; i < 4; i++) {
                                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " " + Integer.toHexString(tRead[i]));
                                }

                            } else {
                                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_read:error ");
                            }
                        } else {
                            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_write:error ");
                        }

                    } else SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_read:error ");

                }
            }

            call_s8.fw_exit(hdev);
        }

    }

    public void printHexString(byte[] b, int length) {
        for (int i = 0; i < length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, hex.toUpperCase());
        }

        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "\n");
    }

    public void testFile(int icdev, byte fID, byte fType, byte[] pSessionKey) {
        int i, status;
        byte[] wdata = new byte[300];//1024];
        byte[] rdata = new byte[300];
        int[] recLen = new int[2];
        int[] rlen = new int[2];
        int[] value = new int[2];
        int credit = 200;
        int debit = 100;

        switch (fType) {
            case 0x00://common standard file

                for (i = 0; i < (int) (fileSize_standard); i++) wdata[i] = (byte) (i + 1);

                status = call_s8.fw_write_desfire(icdev, fID, 0, fileSize_standard, wdata, pSessionKey);
                if (status != 0) {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_write_desfire error!\n");
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, status + "\n");
                    break;
                }
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "standard file write ok \n");

                status = call_s8.fw_read_desfire(icdev, fID, 0, fileSize_standard, rdata, pSessionKey);
                if (status != 0) {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_read_desfire error!\n");
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, status + "\n");
                    break;
                }
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "standard file read ok \n");
                printHexString(rdata, fileSize_standard);

                break;
            case 0x04://record file

                for (i = 0; i < recordSize; i++) wdata[i] = (byte) (i + 1);

                status = call_s8.fw_writeRecord_desfire(icdev, fID, 0, recordSize, wdata, pSessionKey);
                if (status != 0) {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_writeRecord_desfire error!\n");
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, status + "\n");
                    break;
                }
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "record file write ok \n");


                status = call_s8.fw_readRecord_desfire(icdev, fID, 0, 1, rdata, recLen, rlen, pSessionKey);
                if (status != 0) {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_readRecord_desfire error!\n");
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, status + "\n");
                    break;
                }
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "Record file read ok \n");
                printHexString(rdata, rlen[0]);
                break;
            case 0x02://value file

                status = call_s8.fw_getvalue_desfire(icdev, fID, value, pSessionKey);
                if (status != 0) {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_getvalue_desfire error!\n");
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, status + "\n");
                    break;
                }
                System.out.printf("Value file read ok: " + value[0] + " \n");

                status = call_s8.fw_credit_desfire(icdev, fID, credit, pSessionKey);
                if (status != 0) {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_credit_desfire error!\n");
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, status + "\n");
                    break;
                }
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_credit_desfire ok \n");

                status = call_s8.fw_debit_desfire(icdev, fID, debit, pSessionKey);
                if (status != 0) {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_debit_desfire error!\n");
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, status + "\n");
                    break;
                }
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_debit_desfire ok \n");

                status = call_s8.fw_getvalue_desfire(icdev, fID, value, pSessionKey);
                if (status != 0) {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_getvalue_desfire error!\n");
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, status + "\n");
                    break;
                }
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "Value file read ok: " + value[0] + " \n");

                break;

        }
    }

    public void operFiles(int icdev, byte[] fIDs, int fileNum, byte[] pSessionKey) {
        int i;
        byte curFileID;
        byte[] rlen = new byte[8];
        byte[] fileProper = new byte[512];
        int status;


        for (i = 0; i < fileNum; i++) {
            curFileID = fIDs[i];

            status = call_s8.fw_getFileProper(icdev, curFileID, rlen, fileProper);
            if (status != 0) {
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_getFileProper error!\n");
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, status + "\n");

            } else {
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "File " + curFileID + " preporter:\n");
                printHexString(fileProper, rlen[0]);

                testFile(icdev, curFileID, fileProper[0], pSessionKey);
            }

        }
    }

    public void TestDesfire(int portType, String path, int baudrate) {
        // TODO Auto-generated method stub
        int result = 0, hdev = 1;
        char[] pModVer = new char[512];
        int[] fSnr = new int[2];
        long[] sSnr = new long[2];
        char[] pfSnr = new char[8];
        char[] psSnr = new char[8];
        char[] pbufSnr = new char[16];
        char[] pSnr = new char[128];
        char[] pCharSingle = new char[255];
        int i;
        int status;
        short tPage = 4;
        char[] tWrite = {0x01, 0x02, 0x03, 0x04};
        char[] tRead = new char[512];
        byte[] rlen = new byte[8];
        byte[] rdata = new byte[256];
        //Scanner input_cmd;//
        byte[] idApp = {0x01, 0x00, 0x00};
        byte[] accessPermission = {(byte) 0xee, (byte) 0xee};
        byte[] standardFileSize = {(byte) fileSize_standard, 0x00, 0x00};
        byte[] recordFileSize = {(byte) recordSize, 0x00, 0x00};
        byte[] recordFileNum = {0x10, 0x00, 0x00};
        byte[] valueFileMin = {0x00, 0x00, 0x00, 0x00};
        byte[] valueFileMax = {(byte) 0xFF, (byte) 0xFF, 0x00, 0x00};
        byte[] valueDefVal = {0x00, 0x00, 0x00, 0x00};

        byte keyNo = 0;
        byte[] sessionKey = new byte[64];
        byte[] userKey = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,};
        byte[] fileIDs = new byte[512];

        if (portType == PT_USB) hdev = call_s8.fw_init_ex(2, null, 0);
        else hdev = call_s8.fw_init_ex(1, path.toCharArray(), baudrate);
        if (hdev != -1) {

            //try to get module version
            result = call_s8.fw_getver(hdev, pModVer);
            if (0 == result) {
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "-");
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "Module Version: " + String.valueOf(pModVer));

                result = call_s8.fw_card(hdev, (short) 1, fSnr);
                if (0 == result) {
                    result = call_s8.fw_anticoll2(hdev, (byte) 0, sSnr);
                    if (0 == result) {

                        call_s8.fw_select2(hdev, sSnr[0]);

                        pl_javacall.ULToMultiByte(fSnr[0], pfSnr, 4, 0);
                        pl_javacall.ULToMultiByte((int) sSnr[0], psSnr, 4, 0);
                        for (i = 0; i < 3; i++) {
                            pbufSnr[i] = pfSnr[1 + i];
                        }
                        for (i = 0; i < 4; i++) {
                            pbufSnr[3 + i] = psSnr[i];
                        }
                        pl_javacall._hex_a(pSnr, pbufSnr, 14);

                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_card:ok ");
                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, String.valueOf(pSnr));


                        status = call_s8.fw_reset_desfire(hdev, rlen, rdata);
                        if (status != 0) {
                            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_reset desfire error\n");
                            call_s8.fw_exit(hdev);
                            return;
                        }
                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_reset_desfire ok\n");


                        status = call_s8.fw_authen_desfire(hdev, keyNo, userKey, sessionKey);
                        if (status != 0) {
                            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_authen_desfire error \n");
                            call_s8.fw_exit(hdev);
                            return;
                        }

                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_authen_desfire ok \n");
                        call_s8.fw_formatPICC_desfire(hdev);


                        status = call_s8.fw_createApp_desfire(hdev, idApp, (byte) 0x0F, (byte) 0x0E);
                        if (status != 0) {
                            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_createApp_desfire error \n");
                            call_s8.fw_exit(hdev);
                            return;
                        }
                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_createApp_desfire ok \n");


                        status = call_s8.fw_selectApp_desfire(hdev, idApp);
                        if (status != 0) {
                            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_selectApp_desfire error \n");
                            call_s8.fw_formatPICC_desfire(hdev);
                            call_s8.fw_exit(hdev);
                            return;
                        }
                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_selectApp_desfire ok \n");


                        status = call_s8.fw_createDataFile_desfire(hdev, (byte) 0x01, (byte) 0x00, accessPermission, standardFileSize);
                        if (status != 0) {
                            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_createDataFile_desfire error \n");
                            call_s8.fw_formatPICC_desfire(hdev);
                            call_s8.fw_exit(hdev);
                            return;
                        }
                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_createDataFile_desfire ok \n");

                        status = call_s8.fw_createCsyRecord_desfire(hdev, (byte) 0x02, (byte) 0x00, accessPermission, recordFileSize, recordFileNum);
                        if (status != 0) {
                            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_createCsyRecord_desfire error \n");
                            call_s8.fw_formatPICC_desfire(hdev);
                            call_s8.fw_exit(hdev);
                            return;
                        }
                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_createCsyRecord_desfire ok \n");

                        status = call_s8.fw_createValueFile_desfire(hdev, (byte) 0x03, (byte) 0x00, accessPermission, valueFileMin, valueFileMax, valueDefVal, (byte) 0x01);
                        if (status != 0) {
                            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_createValueFile_desfire error \n");
                            call_s8.fw_formatPICC_desfire(hdev);
                            call_s8.fw_exit(hdev);
                            return;
                        }
                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_createValueFile_desfire ok \n");


                        status = call_s8.fw_getFileIDs_desfire(hdev, rlen, fileIDs);
                        if (status != 0) {
                            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_getFileIDs_desfire error \n");
                            call_s8.fw_formatPICC_desfire(hdev);
                            call_s8.fw_exit(hdev);
                            return;
                        }
                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_getFileIDs_desfire ok \n");

                        operFiles(hdev, fileIDs, rlen[0], sessionKey);


                    }
                } else
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " find card failed. ");
            }

            call_s8.fw_exit(hdev);
        } else
            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "fw_init_ex failed");

    }

    public boolean TestCTLcpu(int portType, String path, int baudrate) {
        // TODO Auto-generated method stub
        int result = 0, hdev = 1;
        int[] _Snr = new int[2];
        char[] pLen_char = new char[4];
        char[] rBuf = new char[512];
        int[] pLen_int = new int[1];
        char[] strCmdSend = {0x00, 0x84, 0x00, 0x00, 0x08};// ("0084000008").toCharArray();
        char[] strCmdRev = new char[512];
        char[] strResetInfo = new char[512];
        boolean status = false;
        byte[] btTmp = new byte[512];
        int i;

        if (portType == PT_USB) hdev = call_s8.fw_init_ex(2, null, 0);
        else hdev = call_s8.fw_init_ex(1, path.toCharArray(), baudrate);
        if (hdev != -1) {


            call_s8.fw_reset(hdev, 5);
            result = call_s8.fw_card(hdev, (short) 1, _Snr);
            if (0 == result) {
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "Find card " + _Snr[0] + "(dec)");
                //reset cpu card
                result = call_s8.fw_pro_reset(hdev, pLen_char, pCharSingle);
                if (0 == result) {
                    for (i = 0; i < pLen_char[0]; i++)
                        btTmp[i] = (byte) pCharSingle[i];

                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "Cpu card reset ok->");
                    printHexString(btTmp, pLen_char[0]);

                    //send apdu command
                    result = call_s8.fw_pro_commandlink(hdev, (byte) strCmdSend.length, strCmdSend, pLen_char, strCmdRev, (short) 9, (short) 60);// strCmdRev);
                    if (0 == result) {
                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "contactless cpu card test ok. send->");
                        for (i = 0; i < strCmdSend.length; i++)
                            btTmp[i] = (byte) strCmdSend[i];

                        printHexString(btTmp, strCmdSend.length);

                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "contactless cpu card test ok. rev->");
                        for (i = 0; i < pLen_char[0]; i++)
                            btTmp[i] = (byte) strCmdRev[i];
                        printHexString(btTmp, pLen_char[0]);
                        status = true;
                    } else {
                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " CPU card command sent error.");
                    }
                } else {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " CPU card reset error.");
                }
            } else
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Card not found (contactless cpu card)");

            call_s8.fw_exit(hdev);
        }

        return status;

    }

    public void TestICODE2(int portType, String path, int baudrate) {
        // TODO Auto-generated method stub
        int result = 0, hdev = 1;
        char[] pModVer = new char[512];
        char[] pSnr = new char[255];
        char[] pCharSingle = new char[255];
        char[] pLen_char = new char[4];
        char[] pDataHex = new char[255];
        int i;
        short tPage = 4;
        char[] tWrite = {0x01, 0x02, 0x03, 0x04};
        char[] tRead = new char[512];
        byte[] btTmp = new byte[512];
        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "ICODE2 Test ");
        if (portType == PT_USB) hdev = call_s8.fw_init_ex(2, null, 0);
        else //hdev = call_s8.fw_init_ex (1, path.toCharArray(), baudrate);
        {
            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "The ICODE2 Card is not supported in COM port. ");
            return;
        }
        if (hdev != -1) {

            call_s8.fw_beep(hdev, 5);

            //try to get module version
            result = call_s8.fw_getver(hdev, pModVer);
            if (0 == result) {
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "-");
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "Module Version: " + String.valueOf(pModVer));

                result = call_s8.fw_config_card(hdev, (short) 49);  //15693 card
                if (0 == result) {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_config_card:ok ");
                } else {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_config_card:error ");
                    return;
                }

                result = call_s8.fw_inventory(hdev, (short) 0x36, (short) 0, (short) 0, pLen_char, pSnr);
                if (0 == result) {
                    call_s8.hex_a(pDataHex, pSnr, 16);
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_inventory:ok, cardUID: " + String.valueOf(pDataHex));
                } else {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_inventory:error ");
                    return;
                }

                result = call_s8.fw_select_uid(hdev, (short) 0x22, pSnr);
                if (0 == result) {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_select_uid:ok ");
                } else {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_select_uid:error ");
                    return;
                }

                result = call_s8.fw_writeblock(hdev, (short) 0x22, (short) 6, (short) 1, pSnr, (short) 4, tWrite);//写块6，每次只能写一个块
                if (0 == result) {
                    for (i = 0; i < 16; i++)
                        pDataHex[i] = 0;
                    call_s8.hex_a(pDataHex, tWrite, 8);
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_writeblock:ok, Data: " + String.valueOf(pDataHex));
                } else {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_writeblock:error ");
                    return;
                }

                result = call_s8.fw_readblock(hdev, (short) 0x22, (short) 6, (short) 1, pSnr, pLen_char, tRead);//读块6，每次只能读一个块
                if (0 == result) {
                    for (i = 0; i < 16; i++)
                        pDataHex[i] = 0;
                    call_s8.hex_a(pDataHex, tRead, 8);
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_readblock:ok, Data: " + String.valueOf(pDataHex));
                } else {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_readblock:error ");
                    return;
                }

            }

            call_s8.fw_config_card(hdev, (short) 65);//typeA card
            call_s8.fw_exit(hdev);
        }

    }

    public void TestSecondIDcard(int portType, String path, int baudrate) {
        // TODO Auto-generated method stub
        int result = 0, hdev = 1;
        char[] pModVer = new char[512];
        char[] pATQB = new char[255];
        char[] pLen_char = new char[4];
        char[] pDataHex = new char[255];
        int i;
        char[] tRead = new char[512];
        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "Second ID Card Test ");
        if (portType == PT_USB) hdev = call_s8.fw_init_ex(2, null, 0);
        else //hdev = call_s8.fw_init_ex (1, path.toCharArray(), baudrate);
        {
            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "The Second ID Card is not supported in COM port. ");
            return;
        }
        if (hdev != -1) {

            call_s8.fw_beep(hdev, 5);

            //try to get module version
            result = call_s8.fw_getver(hdev, pModVer);
            if (0 == result) {
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "-");
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "Module Version: " + String.valueOf(pModVer));

                result = call_s8.fw_config_card(hdev, (short) 66);  //typeB card
                if (0 == result) {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_config_card:ok ");
                } else {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_config_card:error ");
                    return;
                }

                result = call_s8.fw_request_b(hdev, (short) 0, (short) 0, (short) 0, pATQB);
                if (0 == result) {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_request_b:ok ");
                } else {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_request_b:error ");
                    return;
                }


                result = call_s8.fw_attrib_ID(hdev, tRead, pLen_char);
                if (0 == result) {
                    for (i = 0; i < 16; i++)
                        pDataHex[i] = 0;
                    call_s8.hex_a(pDataHex, tRead, 16);
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_attrib_ID:ok, the Second ID Card UID: " + String.valueOf(pDataHex));
                } else {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_attrib_ID:error ");
                    return;
                }

            }

            call_s8.fw_config_card(hdev, (short) 65);  //typeA card
            call_s8.fw_exit(hdev);
        }

    }

    public void Test_S8_4442(int portType, String path, int baudrate) {
        // TODO Auto-generated method stub
        int result = 0, hdev = 1;
        char[] pModVer = new char[512];
        int t_offset = 32;
        int t_rwlen = 16;
        char[] tWrite = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0xd, 0xe, 0xf};
        char[] strCmdRev = new char[512];
        char[] defPwd = {0xff, 0xff, 0xff};
        char[] newPwd = {0xff, 0xff, 0xff};
        char[] cntErr = new char[2];

        if (portType == PT_USB) hdev = call_s8.fw_init_ex(2, null, 0);
        else hdev = call_s8.fw_init_ex(1, path.toCharArray(), baudrate);
        if (hdev != -1) {

            call_s8.fw_beep(hdev, 5);

            //try to get module version
            result = call_s8.fw_getver(hdev, pModVer);
            if (0 == result) {
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "-");
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Module Version: " + String.valueOf(pModVer));

                if (0 == result) {

                    result = call_s8.fw_cntReadError_4442(hdev, cntErr);
                    if (result == 0 && cntErr[0] != 0) {
                        result = call_s8.fw_authentikey_4442(hdev, (char) 0, 3, defPwd);

                        if (0 == result) {

                            result = call_s8.fw_write_4442(hdev, (char) t_offset, tWrite, t_rwlen);

                            if (0 == result) {

                                result = call_s8.fw_read_4442(hdev, (char) t_offset, pCharSingle, t_rwlen);
                                if (0 == result) {
                                    call_s8.hex_a(strCmdRev, pCharSingle, t_rwlen * 2);
                                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Read ok, info:" + String.valueOf(strCmdRev));

                                    result = call_s8.fw_changkey_4442(hdev, (char) 0, 3, newPwd);
                                    if (0 == result) {
                                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Change 4442 pwd ok.");
                                    } else {
                                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Change 4442 pwd error.");
                                    }
                                } else {
                                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " IC_Read error.");
                                }
                            }//IC_Write == 0
                            else
                                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Write error.");
                        }//IC_CheckPass_SLE4442 == 0
                        else
                            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Authen password error");
                    }// IC_ReadCount_SLE4442  != 0
                    else
                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Invalid 4442 card.");
                }//IC_Status == 1
                else
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Not find card.");
            }

            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "\r\n");

            call_s8.fw_exit(hdev);
        }

    }

    public void Test_S8_PSAM(int portType, String path, int baudrate) {
        // TODO Auto-generated method stub
        int result = 0, hdev = 1, i;
        char[] pModVer = new char[512];
        byte[] pByte = new byte[512];
        byte[] pLen_char = new byte[1];
        byte[] pLen_int = new byte[1];
        byte[] strCmdSend = {0x00, (byte) 0x84, 0x00, 0x00, 0x08};// ("0084000008").toCharArray();
        char[] strCmdRev = new char[512];

        if (portType == PT_USB) hdev = call_s8.fw_init_ex(2, null, 0);
        else hdev = call_s8.fw_init_ex(1, path.toCharArray(), baudrate);
        if (hdev != -1) {
            //m_tview.setText(" Get Handle: " + Integer.toString(hdev));

            //call_s8.IC_DevBeep(hdev, 10);

            //try to get module version
            result = call_s8.fw_getver(hdev, pModVer);
            if (0 == result) {
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "-");
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Module Version: " + String.valueOf(pModVer));

                call_s8.fw_setcpu(hdev, (byte) 0x0d);

                //reset cpu card
                result = call_s8.fw_cpureset(hdev, pLen_char, pByte);
                if (0 == result) {
                    lenHex = 2 * pLen_char[0];
                    pCharHex = new char[lenHex + 1];

                    for (i = 0; i < pLen_char[0]; i++)
                        pCharSingle[i] = (char) pByte[i];

                    call_s8.hex_a(strCmdRev, pCharSingle, lenHex);
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " CPU card reset ok, info:" + String.valueOf(strCmdRev));

                    strCmdRev = new char[512];

                    //send apdu command
                    result = call_s8.fw_cpuapdu(hdev, (byte) 5, strCmdSend, pLen_int, pByte);
                    if (0 == result) {
                        pLen_int[0] = 10;

                        lenHex = 2 * pLen_int[0];
                        pCharHex = new char[lenHex + 1];

                        for (i = 0; i < pLen_int[0]; i++)
                            strCmdRev[i] = (char) pByte[i];

                        call_s8.hex_a(pCharHex, strCmdRev, lenHex);
                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " CPU card command sent ok, receive info: " + String.valueOf(pCharHex));
                    } else {
                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " CPU card command sent error.");
                    }

                } else {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " CPU card reset error.");
                }
            }

            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "\r\n");

            call_s3.IC_ExitComm(hdev);
        }

    }

    public void Test_S3_Cpu(int portType, String path, int baudrate) {
        // TODO Auto-generated method stub
        int result = 0, hdev = 1;
        char[] pModVer = new char[512];
        char[] pLen_char = new char[1];
        int[] pLen_int = new int[1];
        char[] strCmdSend = {0x00, 0x84, 0x00, 0x00, 0x08};// ("0084000008").toCharArray();
        char[] strCmdRev = new char[512];

        if (portType == PT_USB) hdev = call_s3.IC_InitPort_Ex(2, null, 0);
        else hdev = call_s3.IC_InitPort_Ex(1, path.toCharArray(), baudrate);
        if (hdev != -1) {
            //m_tview.setText(" Get Handle: " + Integer.toString(hdev));

            call_s3.IC_DevBeep(hdev, 10);

            //try to get module version
            result = call_s3.IC_ReadVer(hdev, pModVer);
            if (0 == result) {
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "-");
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Module Version: " + String.valueOf(pModVer));

                call_s3.IC_SetCardSeat(hdev, 0);

                //reset cpu card
                result = call_s3.IC_CpuReset(hdev, pLen_char, pCharSingle);
                if (0 == result) {
                    lenHex = 2 * pLen_char[0];
                    pCharHex = new char[lenHex + 1];
                    call_s3.hex2asc(pCharSingle, strCmdRev, pLen_char[0]);
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " CPU card reset ok, info:" + String.valueOf(strCmdRev));

                    strCmdRev = new char[512];

                    //send apdu command
                    result = call_s3.IC_CpuApdu(hdev, 5, strCmdSend, pLen_int, strCmdRev);
                    if (0 == result) {
                        pLen_int[0] = 10;

                        lenHex = 2 * pLen_int[0];
                        pCharHex = new char[lenHex + 1];
                        call_s3.hex2asc(strCmdRev, pCharHex, pLen_int[0]);
                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " CPU card command sent ok, receive info: " + String.valueOf(pCharHex));

                        //TestHNCos(0x02, call_s3, hdev);//test hncos
                    } else {
                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " CPU card command sent error.");
                    }
                } else {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " CPU card reset error.");
                }
            }

            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "\r\n");

            call_s3.IC_ExitComm(hdev);
        }

    }

    public void Test_S3_PSAM(int portType, String path, int baudrate) {
        // TODO Auto-generated method stub
        int result = 0, hdev = 1;
        char[] pModVer = new char[512];
        char[] pLen_char = new char[1];
        int[] pLen_int = new int[1];
        char[] strCmdSend = {0x00, 0x84, 0x00, 0x00, 0x08};// ("0084000008").toCharArray();
        char[] strCmdRev = new char[512];

        if (portType == PT_USB) hdev = call_s3.IC_InitPort_Ex(2, null, 0);
        else hdev = call_s3.IC_InitPort_Ex(1, path.toCharArray(), baudrate);
        if (hdev != -1) {
            //m_tview.setText(" Get Handle: " + Integer.toString(hdev));

            call_s3.IC_DevBeep(hdev, 10);

            //try to get module version
            result = call_s3.IC_ReadVer(hdev, pModVer);
            if (0 == result) {
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "-");
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Module Version: " + String.valueOf(pModVer));

                call_s3.IC_SetCardSeat(hdev, 1);

                //reset cpu card
                result = call_s3.IC_CpuReset(hdev, pLen_char, pCharSingle);
                if (0 == result) {
                    lenHex = 2 * pLen_char[0];
                    pCharHex = new char[lenHex + 1];
                    call_s3.hex2asc(pCharSingle, strCmdRev, pLen_char[0]);
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " CPU card reset ok, info:" + String.valueOf(strCmdRev));

                    strCmdRev = new char[512];

                    //send apdu command
                    result = call_s3.IC_CpuApdu(hdev, 5, strCmdSend, pLen_int, strCmdRev);
                    if (0 == result) {
                        pLen_int[0] = 10;

                        lenHex = 2 * pLen_int[0];
                        pCharHex = new char[lenHex + 1];
                        call_s3.hex2asc(strCmdRev, pCharHex, pLen_int[0]);
                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " CPU card command sent ok, receive info: " + String.valueOf(pCharHex));
                    } else {
                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " CPU card command sent error.");
                    }

                    //TestHNCos(0x02, call_s3, hdev);
                } else {
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " CPU card reset error.");
                }
            }

            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "\r\n");

            call_s3.IC_ExitComm(hdev);
        }

    }

    public void Test_S3_4442(int portType, String path, int baudrate) {
        // TODO Auto-generated method stub
        int result = 0, hdev = 1;
        char[] pModVer = new char[512];
        int t_offset = 32;
        int t_rwlen = 16;
        char[] tWrite = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0xd, 0xe, 0xf};
        char[] strCmdRev = new char[512];
        char[] defPwd = {0xff, 0xff, 0xff};
        char[] newPwd = {0xff, 0xff, 0xff};
        //char[] pCharSingle_write = {0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,0x11,
        //                            0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,0x22,
        //                            0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,0x33,
        //                            0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,0x44,
        //                            0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,0x55,};//
        char[] pCharSingle_write = new char[1024];
        String write_contetn = "A2131091FFFF8115FFFFFFFFFFFFFFFFFFFF01FFFFD27600000400FFFFFFFFFF501807180000000000000000002A006400C80000009001F4010000F401120712060C18071800000C01FFFFFFFFFFFF3AFFFFFFFFFFFFFFFF00FFFFFFFFFFFFFF91014000204381000001000092071F0000000050F80C006400C8009001F401F401120712060C00FFFF0000842801A428FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFB2BDDEF61283F566CA35B42AEB3300FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF0000000000FFFFFFFFFFFFFFFF";

        if (portType == PT_USB) hdev = call_s3.IC_InitPort_Ex(2, null, 0);
        else hdev = call_s3.IC_InitPort_Ex(1, path.toCharArray(), baudrate);
        if (hdev != -1) {
            //m_tview.setText(" Get Handle: " + Integer.toString(hdev));

            call_s3.IC_DevBeep(hdev, 10);

            //try to get module version
            result = call_s3.IC_ReadVer(hdev, pModVer);
            if (0 == result) {
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "-");
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Module Version: " + String.valueOf(pModVer));

                if (portType == PT_USB) {
                    result = call_s3.IC_WriteDevice(hdev, t_offset, t_rwlen, tWrite);//写读写器EEPROM

                    if (0 == result) {

                        result = call_s3.IC_ReadDevice(hdev, t_offset, t_rwlen, pCharSingle);//读读写器EEPROM
                        if (0 == result) {
                            call_s3.hex2asc(pCharSingle, strCmdRev, t_rwlen);
                            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Read EEPROM ok, info:" + String.valueOf(strCmdRev));

                            //result = call_s3.IC_ChangePass_SLE4442(hdev,newPwd);
                            //if(0 == result)
                            //{
                            //	SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND," Change 4442 pwd ok.");
                            //}
                            //else
                            //{
                            //	SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND," Change 4442 pwd error.");
                            //}
                        } else {
                            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " IC_ReadDevice error.");
                        }
                    }//IC_Write == 0
                    else
                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " IC_WriteDevice error.");

                }

                result = call_s3.IC_Status(hdev);
                if (((1 == result) && (portType == PT_USB)) || ((0 == result) && (portType == PT_SERIAL))) {
                    call_s3.IC_Choose_4442(hdev);

                    result = call_s3.IC_ReadCount_SLE4442(hdev);
                    if (result != 0) {
                        result = call_s3.IC_CheckPass_SLE4442(hdev, defPwd);

                        if (0 == result) {

                            //result = call_s3.IC_Write(hdev, t_offset, t_rwlen, tWrite );
                            call_s3.asc2hex(write_contetn.substring(64, 512).toCharArray(), pCharSingle_write, 224);
                            result = call_s3.IC_Write(hdev, 32, 224, pCharSingle_write);//写卡

                            if (0 == result) {

                                //result = call_s3.IC_Read(hdev, t_offset, t_rwlen, pCharSingle);
                                result = call_s3.IC_Read(hdev, 32, 224, pCharSingle);
                                if (0 == result) {
                                    //call_s3.hex2asc(pCharSingle, strCmdRev, t_rwlen );
                                    call_s3.hex2asc(pCharSingle, strCmdRev, 224);
                                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Read ok, info:" + String.valueOf(strCmdRev));

                                    //result = call_s3.IC_ChangePass_SLE4442(hdev,newPwd);
                                    //if(0 == result)
                                    //{
                                    //	SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND," Change 4442 pwd ok.");
                                    //}
                                    //else
                                    //{
                                    //	SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND," Change 4442 pwd error.");
                                    //}
                                } else {
                                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " IC_Read error.");
                                }
                            }//IC_Write == 0
                            else
                                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Write error.");
                        }//IC_CheckPass_SLE4442 == 0
                        else
                            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Authen password error");
                    }// IC_ReadCount_SLE4442  != 0
                    else
                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Invalid 4442 card.");
                }//IC_Status == 1
                else
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Not find card.");
            }

            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "\r\n");

            if (portType == PT_USB) {
                call_s3.IC_PwrDown(hdev);  //卡片下电
                call_s3.IC_PwrRst(hdev);   //卡片上电
            }

            call_s3.IC_ExitComm(hdev);
        }

    }

    public void Test_S3_4428(int portType, String path, int baudrate) {
        // TODO Auto-generated method stub
        int result = 0;
        char[] pModVer = new char[512];
        int t_offset = 32;
        int t_rwlen = 16;
        char[] tWrite = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0xd, 0xe, 0xf};
        char[] strCmdRev = new char[512];
        char[] defPwd = {0xff, 0xff};
        char[] newPwd = {0xff, 0xff};

        //if(hdev == -1)
        //{
        if (portType == PT_USB) hdev = call_s3.IC_InitPort_Ex(2, null, 0);
        else hdev = call_s3.IC_InitPort_Ex(1, path.toCharArray(), baudrate);
        //}
        if (hdev != -1) {
            //m_tview.setText(" Get Handle: " + Integer.toString(hdev));

            call_s3.IC_DevBeep(hdev, 3);


            //try to get module version
            result = call_s3.IC_ReadVer(hdev, pModVer);
            if (0 == result) {
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "-");
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Module Version: " + String.valueOf(pModVer));

                result = call_s3.IC_Status(hdev);
                if (((1 == result) && (portType == PT_USB)) || ((0 == result) && (portType == PT_SERIAL))) {
                    call_s3.IC_Choose_4428(hdev);

                    result = call_s3.IC_ReadCount_SLE4428(hdev);
                    if (result != 0) {
                        result = call_s3.IC_CheckPass_SLE4428(hdev, defPwd);

                        if (0 == result) {

                            result = call_s3.IC_Write(hdev, t_offset, t_rwlen, tWrite);

                            if (0 == result) {

                                result = call_s3.IC_Read(hdev, t_offset, t_rwlen, pCharSingle);
                                if (0 == result) {
                                    call_s3.hex2asc(pCharSingle, strCmdRev, t_rwlen);
                                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Read ok, info:" + String.valueOf(strCmdRev));

                                    result = call_s3.IC_ChangePass_SLE4428(hdev, newPwd);
                                    if (0 == result) {
                                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Change 4428 pwd ok.");
                                    } else {
                                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Change 4428 pwd error.");
                                    }
                                } else {
                                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " IC_Read error.");
                                }
                            }//IC_Write == 0
                            else
                                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Write error.");
                        }//IC_CheckPass_SLE4442 == 0
                        else
                            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Authen password error");
                    }// IC_ReadCount_SLE4442  != 0
                    else
                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Invalid 4428 card.");
                }//IC_Status == 1
                else
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Not find card." + result);
            }

            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "\r\n");


            call_s3.IC_ExitComm(hdev);
        }

    }

    public void Test_S3_24Cxx(int portType, String path, int baudrate) {
        // TODO Auto-generated method stub
        int result = 0;
        char[] pModVer = new char[512];
        int t_offset = 32;
        int t_rwlen = 16;
        char[] tWrite = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0xd, 0xe, 0xf};
        char[] strCmdRev = new char[512];

        if (portType == PT_USB) return; //hdev = call_s3.IC_InitPort_Ex(2, null, 0); //暂时不支持usb
        else hdev = call_s3.IC_InitPort_Ex(1, path.toCharArray(), baudrate);

        if (hdev != -1) {
            //m_tview.setText(" Get Handle: " + Integer.toString(hdev));

            call_s3.IC_DevBeep(hdev, 3);


            //try to get module version
            result = call_s3.IC_ReadVer(hdev, pModVer);
            if (0 == result) {
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "-");
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Module Version: " + String.valueOf(pModVer));

                result = call_s3.IC_Status(hdev);
                if (((1 == result) && (portType == PT_USB)) || ((0 == result) && (portType == PT_SERIAL))) {
                    call_s3.IC_Choose_24Cxx(hdev);

                    result = call_s3.IC_Write(hdev, t_offset, t_rwlen, tWrite);

                    if (0 == result) {

                        result = call_s3.IC_Read(hdev, t_offset, t_rwlen, pCharSingle);
                        if (0 == result) {
                            call_s3.hex2asc(pCharSingle, strCmdRev, t_rwlen);
                            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Read ok, info:" + String.valueOf(strCmdRev));
                        } else {
                            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " IC_Read error.");
                        }
                    }//IC_Write == 0
                    else
                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Invalid AT24Cxx card.");
                }//IC_Status == 1
                else
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Not find card." + result);
            }

            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "\r\n");


            call_s3.IC_ExitComm(hdev);
        }

    }

//	private void CiperTest()
//	{
//		pl_javacall publib_ = new  pl_javacall();
//		char defKey[] =  new char[]{'F','F','F','F','F','F','F','F','F','F','F','F','F','F','F','F','F','F','F','F','F','F','F','F','F','F','F','F','F','F','F','F'};
//		char cmdbuf[] = new char[]{0x12,0x34,0x44,0x44,0x55,0x41,0x42,0x14,0x89};
//		int ptrdatelen = 9;
//		char MAC[] = new char[9];
//		char challenge[] = new char[]{0x12,0x22,0x33,0x44,0x55,0x66,0x77,0x88};
//		int cnt = 0;
//		char ptrdate[] = new char[128];
//	
//		while(true)
//		{
//			//publib_.ic_encrypt_3DES_ECB(defKey,cmdbuf,( short)ptrdatelen,ptrdate); 
//			publib_.ic_encrypt_3DES_ECB_MAC(defKey,cmdbuf,( short)(ptrdatelen),MAC,challenge);  //璁＄畻MAC
//			SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND,"mac_\n");
//			if(cnt == 100)break;
//			cnt++;
//		}
//		
//	}

    public void Test_S3_102(int portType, String path, int baudrate) {
        // TODO Auto-generated method stub
        int result = 0;
        char[] pModVer = new char[512];
        int t_offset = 32;
        int t_rwlen = 16;
        char[] tWrite = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0xd, 0xe, 0xf};
        char[] strCmdRev = new char[512];
        char[] defPwd = {0xff, 0xff};
        char[] newPwd = {0xff, 0xff};
        char[] errCnt = new char[10];

        //if(hdev == -1)
        //{
        if (portType == PT_USB) return; //hdev = call_s3.IC_InitPort_Ex(2, null, 0);
        else hdev = call_s3.IC_InitPort_Ex(1, path.toCharArray(), baudrate);
        //}
        if (hdev != -1) {
            //m_tview.setText(" Get Handle: " + Integer.toString(hdev));

            call_s3.IC_DevBeep(hdev, 3);


            //try to get module version
            result = call_s3.IC_ReadVer(hdev, pModVer);
            if (0 == result) {
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "-");
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Module Version: " + String.valueOf(pModVer));

                result = call_s3.IC_Status(hdev);
                if (((1 == result) && (portType == PT_USB)) || ((0 == result) && (portType == PT_SERIAL))) {
                    call_s3.IC_Choose_102(hdev);

                    result = call_s3.IC_ReadErrorCnt_102(hdev, errCnt);
                    if (result == 0 && errCnt[0] != 0) {
                        result = call_s3.IC_CheckUserCode_102(hdev, defPwd);

                        if (0 == result) {
                            call_s3.IC_Erase(hdev, t_offset, t_rwlen); //102UserCode_102卡写卡前需先擦除

                            result = call_s3.IC_Write(hdev, t_offset, t_rwlen, tWrite);

                            if (0 == result) {

                                result = call_s3.IC_Read(hdev, t_offset, t_rwlen, pCharSingle);
                                if (0 == result) {
                                    call_s3.hex2asc(pCharSingle, strCmdRev, t_rwlen);
                                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Read ok, info:" + String.valueOf(strCmdRev));

                                    result = call_s3.IC_ChangeUserCode_102(hdev, newPwd);
                                    if (0 == result) {
                                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Change 102 pwd ok.");
                                    } else {
                                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Change 102 pwd error.");
                                    }
                                } else {
                                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " IC_Read error.");
                                }
                            }//IC_Write == 0
                            else
                                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Write error.");
                        }//IC_CheckUserCode_102 == 0
                        else
                            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Authen password error");
                    }// IC_ReadErrorCnt_102  != 0
                    else
                        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Invalid AT88SC102 card.");
                }//IC_Status == 1
                else
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, " Not find card." + result);
            }

            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "\r\n");


            call_s3.IC_ExitComm(hdev);
        }

    }

    private void TestHNCos(int devType, function_S3 s3, int hdev) {
        obj_hncos = new HNCOS(devType, s3);
        int st;
        char defKey[] = new char[]{'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F', 'F'};
        char szMFName[] = new char[]{'D', '1', '5', '6', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '5', '0', '0'};
        char IDKeyEF_MF[] = new char[]{'0', '0', '0', '1'};
        char EFTypeKey[] = new char[]{'0', '5'};
        char KeyTypeEx[] = new char[]{'0', '8'};
        char KeyTypeMaintain[] = new char[]{'0', '5'};
        char EFTypeBin[] = new char[]{'0', '0'};
        char LenKeyEF[] = new char[]{'2', '0', '0', '0'};
        char LenBinEF_DF01[] = new char[]{'1', '0', '0', '0'};//16 bytes
        char IDBinEF_DF02[] = new char[]{'0', '0', '0', '6'};
        char LenBinEF_DF02[] = new char[]{'0', '0', '0', '4'};//"1000";//4K
        char szOffset[] = new char[]{'0', '0', '0', '0'}; //0x0000->0 start address
        char szRWLen[] = new char[]{'0', '5'};//0x0A->10 bytes
        int iRWLen = 0x05;//0x20;
        char szRData[] = new char[1024];
        char bufWData[] = new char[]{'1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
        char bufRData[] = new char[128];
        char szWData[] = new char[1024];

        /* 寤虹珛涓诲簲鐢�*/

        st = obj_hncos.FWCosCreateMF(hdev, szMFName, 20);
        if (st != 0) {
            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "cos test err: FWCosCreateMF: code:" + st + "\n");
            return;
        }
        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "cos test FWCosCreateMF ok\n");



        /* 寤虹珛瀵嗛挜鏂囦欢 */
        st = obj_hncos.FWCosCreateEF(
                hdev,
                IDKeyEF_MF,
                EFTypeKey,
                LenKeyEF,
                0,
                0);
        if (st != 0) {
            obj_hncos.FWCosDeleteMF(hdev);
            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "cos test err: FWCosCreateEF: code:" + st + "\n");
            return;
        }
        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "cos test FWCosCreateEF ok\n");

        /* 瀹夎澶栭儴璁よ瘉瀵嗛挜 */
        st = obj_hncos.FWCosWriteKEY(
                hdev,
                KeyTypeEx,
                defKey, //MFKey,
                32,
                0,
                null);//defKey);
        if (st != 0) {
            obj_hncos.FWCosDeleteMF(hdev);
            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "cos test err: FWCosWriteKEY: code:" + st + "\n");
            return;
        }
        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "cos test FWCosWriteKEY ok\n");


        /* 瀹夎搴旂敤缁存姢瀵嗛挜 */
        st = obj_hncos.FWCosWriteKEY(
                hdev,
                KeyTypeMaintain,
                defKey, //AppKey
                32,
                0,
                null);
        if (st != 0) {
            obj_hncos.FWCosDeleteMF(hdev);
            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "cos test err: FWCosWriteKEY: code:" + st + "\n");
            return;
        }
        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "cos test FWCosWriteKEY ok\n");


        /* 鍒涘缓浜岃繘鍒舵枃浠�*/
        st = obj_hncos.FWCosCreateEF(
                hdev,
                IDBinEF_DF01,
                EFTypeBin,
                LenBinEF_DF01,
                1,
                1);
        if (st != 0) {
            obj_hncos.FWCosDeleteMF(hdev);
            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "cos test err: FWCosCreateEF: code:" + st + "\n");
            return;
        }
        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "cos test FWCosCreateEF ok\n");


		/* 鍒涘缓 涓嬬殑浜岃繘鍒舵枃浠�/
		st = obj_hncos. FWCosCreateEF(
			hdev,
			IDBinEF_DF02,
			EFTypeBin,
			LenBinEF_DF02,
			1,
			1);
		if(st !=0)
		{
			obj_hncos.FWCosDeleteMF(hdev);
			SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND,"cos test err: FWCosCreateEF: code:"+st +"\n");
			return ;
		}
		SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND,"cos test FWCosCreateEF ok\n");


		/* 婵�椿 MF */
        st = obj_hncos.FWCosCreateEndMF(
                hdev);
        if (st != 0) {
            obj_hncos.FWCosDeleteMF(hdev);
            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "cos test err: FWCosCreateEndMF: code:" + st + "\n");
            return;
        }
        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "cos test FWCosCreateEndMF ok\n");



        /* 楠岃瘉涓绘帶瀵嗛挜 */
        st = obj_hncos.FWCosExternalAuthentication(
                hdev,
                defKey,//MFKey,
                32);
        if (st != 0) {
            obj_hncos.FWCosDeleteMF(hdev);
            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "cos test err: FWCosExternalAuthentication: code:" + st + "\n");
            return;
        }

        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "cos test FWCosExternalAuthentication ok\n");

        /* 閫夋嫨鏂囦欢 */
        st = obj_hncos.FWCosSelectFile(
                hdev,
                FileType_EF,
                IDBinEF_DF01);
        if (st != 0) {
            obj_hncos.FWCosDeleteMF(hdev);
            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "cos test err: FWCosSelectFile: code:" + st + "\n");
            return;
        }

        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "cos test FWCosSelectFile ok\n");

        /* write data */
        publib._hex_a(szWData, bufWData, iRWLen * 2);

        st = obj_hncos.FWCosUpdateBinaryEx(
                hdev,
                szOffset,
                szWData,
                iRWLen * 2,
                (int) 1,//0,
                defKey);
        if (st != 0) {
            obj_hncos.FWCosDeleteMF(hdev);
            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "cos test err: FWCosUpdateBinaryEx: code:" + st + "\n");
            return;
        }
        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "cos test FWCosUpdateBinaryEx ok\n");

		/* 璇绘暟鎹�/
		st =obj_hncos. FWCosReadBinaryEx(
			hdev,
			szOffset,
			szRWLen,
			szRData);
		if(st !=0)
		{
			obj_hncos.FWCosDeleteMF(hdev);
			SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND,"cos test err: FWCosReadBinaryEx: code:"+st +"\n");
			return;
		}
		publib._a_hex(bufRData, szRData, iRWLen);
		SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND,"cos test FWCosReadBinaryEx ok\n");
		SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND,"data:[asc]:"+String.valueOf(szRData)+" [hex]:"+String.valueOf(bufRData)+"\n");

		/* delete MF */
        st = obj_hncos.FWCosDeleteMF(hdev);
        if (st != 0) {
            SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "cos test err: FWCosDeleteMF: code:" + st + "\n");
            return;
        }
        SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "cos test FWCosDeleteMF ok\n");

    }

    public void TestFuncKB(int portType, String path, int baudrate) {
        // TODO Auto-generated method stub
        int result = 0, hdev = 1;

        if (portType == PT_USB) hdev = call_s8.fw_init_ex(2, null, 0);
        else hdev = call_s8.fw_init_ex(1, path.toCharArray(), baudrate);
        if (hdev != -1) {
            //m_tview.setText(" Get Handle: " + Integer.toString(hdev));

            short status;
            short TIMELEN = 160;
            char[] pRBuffer = new char[256];
            char[] pRlen = new char[1];
            char[] showstr = new char[100];


            call_s8.fw_lcd_dispclear(hdev);

            status = call_s8.fw_PassIn(hdev, (short) TIMELEN);

            if (status != 0) {
                System.out.print("fw_PassIn error!\n");
            } else {
                System.out.println("please input your password in " + TIMELEN + " seconds![Press ENTER after Input]\n");//strmsg);
            }

            do {

                java.util.Arrays.fill(pRBuffer, '\0');

                status = call_s8.fw_PassGet(hdev, pRlen, pRBuffer);

                if (status != 0Xa1 && status != 0xa2 && status != 0xa5 && status != 0x00) {
                    call_s8.fw_CheckKeyValue(hdev, pRlen, pRBuffer);

                    if ((byte) (pRlen[0]) % 2 != 0)
                        pRBuffer[(byte) pRlen[0]] = 0x20;


                    call_s8.fw_lcd_dispstr(hdev, pRBuffer);
                }

            }
            while (status != 0Xa1 && status != 0xa2 && status != 0xa5 && status != 0x00);


            switch (status) {
                case 0x00:

                    int i = 0;

                    call_s8.fw_lcd_dispclear(hdev);

                    pRlen[0] -= 1;

                    System.out.println("\nYour password is:");

                    for (i = 0; i < (byte) pRlen[0]; i++) System.out.print((char) pRBuffer[i]);
                    System.out.print("\n");

                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "password: " + String.valueOf(pRBuffer) + "\n");

                    break;

                case 0xa1:

                    System.out.print("Your password is too length.(should less then 25).");

                    break;

                case 0xa2:

                    System.out.print("You cancel your input.ReInput Please!\n");

                    break;

                case 0xa5:

                    System.out.print("TimeOUT!\n");

                    break;
            }

            call_s8.fw_exit(hdev);
        }

    }

    ////////////////////////////////////////////////////////////////// AUTO TEST ////
    public boolean AutoTestM1(int hdev, int curCnt) {
        // TODO Auto-generated method stub
        int result = 0;
        char[] pSnrM1 = new char[255];
        boolean st = false;

        if (hdev != -1) {

            result = call_s8.fw_card_str(hdev, (short) 0, pSnrM1);
            if (0 == result) {
                //call_s8.fw_beep(hdev,5);
                SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, "_card:ok[" + String.valueOf(curCnt) + "] ID:" + String.valueOf(pSnrM1) + ".Move away the card please.");

                call_s8.fw_halt(hdev);
                st = true;
            }
        }

        return st;
    }

    public boolean AutoTestS3_4428(int hdev, int curCnt) {
        // TODO Auto-generated method stub
        int result = 0;
        char[] pSnrM1 = new char[255];
        boolean st = false;
        int t_offset = 32;
        int t_rwlen = 16;
        char[] strCmdRev = new char[512];

        if (hdev != -1) {

            result = call_s3.IC_Status(hdev);
            if (((1 == result) && (struct_portType == PT_USB)) || ((0 == result) && (struct_portType == PT_SERIAL))) {
                call_s3.IC_Choose_4428(hdev);

                result = call_s3.IC_Read(hdev, t_offset, t_rwlen, pCharSingle);
                if (0 == result) {
                    //call_s3.hex2asc(pCharSingle, strCmdRev, t_rwlen );
                    SendUIMessage(UI_UPDATE_MSG_TEXT_APPEND, String.valueOf(curCnt));

                    st = true;
                }
            }
        }

        return st;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
     //   getMenuInflater().inflate(R.menu.activity_l, menu);
        return true;
    }

    private class AutoTestThread extends Thread {

        int hdev = 1;
        String devPath;
        int baud;
        int okCnt = 0;
        String strNoCardMsg = "put on card...";

        @Override
        public void run() {
            try {

                devPath = m_text_devPath.getText().toString();
                baud = Integer.parseInt(m_text_baud.getText().toString());

                gl_autoRun = 1;

                SendUIMessage(UI_UPDATE_BTN_MANUAL_DISABLE, "");
                SendUIMessage(UI_UPDATE_BTN_AUTO, "stop");

                while (gl_autoRunning == 1) {

                    Thread.sleep(500);//50

                    if (1 == gl_singleTestInAutoRunning)
                        continue;

                    gl_singleTestInAutoRunning = 1;

                    DoOneTest();

                    gl_singleTestInAutoRunning = 0;

                }

                gl_autoRun = 0;
                SendUIMessage(UI_UPDATE_BTN_AUTO, "AutoTest");
                SendUIMessage(UI_UPDATE_BTN_MANUAL_ENABLE, "");

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }


}
