public class userApplication {

    import ithakimodem.*;
    import java.io.*;
    import java.util.*;

    static String echo_request_code = "E4221\r";
    static String image_free_request_code = "M2766\r";
    static String image_error_request_code = "G5289\r";
    static String gps_request_code = "P8634";
    static String ACK_request_code = "Q9570\r";
    static String NACK_request_code = "R0943\r";

    public static void main(String[] args) throws IOException {
        (new userApplication()).features();
    }
    // Initialize the modem and read the intoductory message
    public static Modem modem_init() {
        Modem modem;
        modem = new Modem();
        modem.setSpeed(80000);
        modem.setTimeout(2000);
        modem.open("ithaki");
        String stranger = "";
        int k;
        for (;;) {
            k = modem.read();
            if (k != -1)
                stranger += (char) k;
            else
                break;
            if (stranger.indexOf("\r\n\n\n") > -1) {
                break;
            }
        }
        return modem;
    }

    public void features() throws IOException {
        System.out.println("The Session has started at " + LocalDateTime.now());
        Modem modem = modem_init();
        ////// Echo Packages Receive
        EchoPackages(modem);
        ///// ARQ
        ARQerror(modem);
        ////// Image Without Error
        ImageFreeError(modem);
        ////// Image With Error
        ImageWithError(modem);
        ////// Gps Request with T
        GPSCoordinatesWithT(modem, gps_request_code);
        ////// Gps Request with R
        GPSCoordinatesWithR(modem, gps_request_code);

        System.out.println("The Session has ended at " + LocalDateTime.now());

    }

    public static void EchoPackages(Modem modem) throws FileNotFoundException {
        byte[] echoreq = echo_request_code.getBytes();

        int k;
        int packages = 0;

        File Output_from_echo = new File("Session1/Echo.txt");
        File Time_for_echo = new File("Session1/Time.txt");

        FileOutputStream echo_stream = new FileOutputStream(Output_from_echo);
        FileOutputStream time_stream = new FileOutputStream(Time_for_echo);

        // Start to measure the time of the exchange
        long begin = System.currentTimeMillis();
        long start, end;
        String response = "";
        String echostring = "";
        while ((System.currentTimeMillis() - begin) < 300000) // exchange packages for 5 minutes 300000ms
        {
            String inside_text = "";
            start = System.currentTimeMillis();
            modem.write(echoreq);
            for (;;) {
                k = modem.read();
                if (k != -1) {
                    inside_text += (char) k;
                    if (inside_text.indexOf("PSTOP") > -1) {
                        packages += 1;
                        end = System.currentTimeMillis();
                        response += Long.toString(end - start) + "\r\n";
                        echostring += inside_text + "\r\n";
                        break;
                    }
                } else
                    break;
            }

        }
        System.out.println("A total of: " + packages + " packages has been received.");
        System.out.println("G1 at: " + LocalDateTime.now());
        try {
            echo_stream.write(echostring.getBytes());
            time_stream.write(response.getBytes());
            echo_stream.close();
            time_stream.close();
        } catch (IOException x) {
            System.out.println(x);
        }

    }

    public static void ImageFreeError(Modem modem) throws FileNotFoundException {
        byte[] imgreq = image_free_request_code.getBytes();
        modem.write(imgreq);

        File image_free = new File("Session1/image_free.jpeg");
        FileOutputStream img_free_stream = new FileOutputStream(image_free);

        int pixel_cur;
        int last_pixel = 0;
        boolean image_Receive_flag = false;

        ArrayList<Byte> img_free = new ArrayList<Byte>();
        //Check the buffer flow, until you find the "start of image" delimiter
        do {
            pixel_cur = modem.read();
            if (pixel_cur == 255) {
                last_pixel = pixel_cur;
                pixel_cur = modem.read();
                if (pixel_cur == 216) {
                    image_Receive_flag = true;
                    System.out.println("Image Receiving has been started!");

                    img_free.add((byte) last_pixel);
                    img_free.add((byte) pixel_cur);
                }
            }
        } while (!image_Receive_flag);

        if (!image_Receive_flag) {
            System.err.println("Exit! Start of Image delimiter is missing.");
            System.exit(0);
        }
        for (;;) {
            pixel_cur = modem.read();

            if (pixel_cur == -1)
                break;
            //Check the incoming bytes, until you find the "end of image" delimiter
            if (last_pixel == 255 && pixel_cur == 217) {
                System.out.println("End of image receiving");
                img_free.add((byte) pixel_cur);
                break;
            }
            img_free.add((byte) pixel_cur);
            last_pixel = pixel_cur;
        }
        System.out.println("Image without error at " + LocalDateTime.now());
        //convert the arraylist of the incoming bytes to byte[] and -try- to write them to the .jpeg file.
        try {
            img_free_stream.write(convertBytes(img_free));
            img_free_stream.close();
        } catch (Exception x) {
            System.out.println(x);
        }
    }

    public static void ImageWithError(Modem modem) throws FileNotFoundException {
        byte[] imgreq = image_error_request_code.getBytes();
        modem.write(imgreq);

        File image_with_error = new File("Session1/image_with_error.jpeg");
        FileOutputStream img_error_stream = new FileOutputStream(image_with_error);

        int pixel_cur;
        int last_pixel = 0;
        boolean image_Receive_flag = false;

        ArrayList<Byte> img_with_error = new ArrayList<Byte>();
        //Check the buffer flow, until you find the "start of image" delimiter
        do {
            pixel_cur = modem.read();
            if (pixel_cur == 255) {
                last_pixel = pixel_cur;
                pixel_cur = modem.read();
                if (pixel_cur == 216) {
                    image_Receive_flag = true;
                    System.out.println("Image Receiving (with errors) has been started!");

                    img_with_error.add((byte) last_pixel);
                    img_with_error.add((byte) pixel_cur);
                }
            }
        } while (!image_Receive_flag);

        if (!image_Receive_flag) {
            System.err.println("Exit! Start of Image delimiter is missing.");
            System.exit(0);
        }
        for (;;) {
            pixel_cur = modem.read();

            if (pixel_cur == -1)
                break;
            //Check the incoming bytes, until you find the "end of image" delimiter
            if (last_pixel == 255 && pixel_cur == 217) {
                System.out.println("End of image receiving");
                img_with_error.add((byte) pixel_cur);
                break;
            }
            img_with_error.add((byte) pixel_cur);
            last_pixel = pixel_cur;
        }
        System.out.println("Image with error at " + LocalDateTime.now());
        //convert the arraylist of the incoming bytes to byte[] and -try- to write them to the .jpeg file.
        try {
            img_error_stream.write(convertBytes(img_with_error));
            img_error_stream.close();
        } catch (Exception x) {
            System.out.println(x);
        }

    }

    public static void GPSCoordinatesWithT(Modem modem, String req) throws FileNotFoundException {
        //String GpsPoints = "T=225740403733T=225744403728T=225751403741T=225736403746T=225722403750\r"; //Session 1
        String GpsPoints = "T=225740403736T=225744403724T=225751403728T=225736403733T=225722403741\r"; //Session 2
        req = req + GpsPoints;
        byte[] gpsreq = req.getBytes();
        modem.write(gpsreq);

        int k;
        int last_k = 0;
        boolean image_gps_flag = false;
        ArrayList<Byte> img_with_gps = new ArrayList<Byte>();

        File image_with_gps = new File("Session1/image_with_gps.jpeg");
        FileOutputStream img_gps_stream = new FileOutputStream(image_with_gps);

        do {
            k = modem.read();
            if (k == 255) {
                last_k = k;
                k = modem.read();
                if (k == 216) {
                    image_gps_flag = true;
                    System.out.println("Gps Image Receiving has been started!");

                    img_with_gps.add((byte) last_k);
                    img_with_gps.add((byte) k);
                }
            }
        } while (!image_gps_flag);

        for (;;) {
            k = modem.read();
            if (k == -1)
                break;
            if (last_k == 255 && k == 217) {
                System.out.println("End of Gps image receiving");
                img_with_gps.add((byte) k);
                break;
            }
            img_with_gps.add((byte) k);
            last_k = k;
        }
        System.out.println("GPS with T at " + LocalDateTime.now());
        try {
            img_gps_stream.write(convertBytes(img_with_gps));
            img_gps_stream.close();
        } catch (Exception x) {
            System.out.println(x);
        }
    }

    public static void GPSCoordinatesWithR(Modem modem, String req) throws IOException {
        //String GpsPoints = "R=1008099\r";// Session 1
        String GpsPoints = "R=1010099\r";// Session 2
        req = req + GpsPoints;
        byte[] gpsreq =req.getBytes();
        modem.write(gpsreq);

        int k;
        double num;
        String gps_txt = "";
        String temp_string;
        String[] per_line;
        String Amplitude;
        String Length;

        File Output_from_gps_req = new File("Session1/Gps_req.txt");

        FileOutputStream Gps_req_stream = new FileOutputStream(Output_from_gps_req);
        String GpsPoints_mine = "";
        for (;;) {
            k = modem.read();
            if (k == -1)
                break;
            gps_txt += (char) k;
        }

        try {
            Gps_req_stream.write(gps_txt.getBytes());
            Gps_req_stream.close();
        } catch (IOException x) {
            System.out.println(x);
        }
        // Split the string with the gps info, at every new line.
        per_line = gps_txt.split("\r\n");
        int count = 0;
        double time = 0, curr_time = 0;
        // For every line with the tag "$GPGGA", find the substring which contains the info about time.
        if (per_line[1].indexOf("$GPGGA") > -1) {
            temp_string = per_line[1].substring(per_line[1].indexOf("$GPGGA") + 7, per_line[1].indexOf("$GPGGA") + 13);
            time = Double.parseDouble(temp_string);
        }

        int index = 1;
        for (int requests = 0; requests < 5; requests++) {
            curr_time = time;
            //For those lines, find the substring which contains the info about amplitude.
            temp_string = per_line[index].substring(per_line[index].indexOf("A") + 13,
                    per_line[index].indexOf("N") - 1);
            num = Double.parseDouble(temp_string);
            Amplitude = convertCoordinates(num);
            // and  the substring which contains the info about length.
            temp_string = per_line[index].substring(per_line[index].indexOf("N") + 2, per_line[index].indexOf("E") - 1);
            num = Double.parseDouble(temp_string);
            Length = convertCoordinates(num);
            GpsPoints_mine += "T=" + Length + Amplitude;
            // check for the sequentially points, to differ 15 seconds.
            while (count < 99 && time - curr_time < 15) {
                index = 1 + count;
                if (per_line[index].indexOf("$GPGGA") > -1) {
                    temp_string = per_line[index].substring(per_line[index].indexOf("$GPGGA") + 7,
                            per_line[index].indexOf("$GPGGA") + 13);
                    time = Double.parseDouble(temp_string);
                }
                count++;
            }
        }
        GpsPoints_mine += "\r";
        String new_req = gps_request_code + GpsPoints_mine;
        byte[] gpsnewreq = new_req.getBytes();
        modem.write(gpsnewreq);

        int last_k = 0;
        boolean image_gps_flag = false;
        ArrayList<Byte> img_with_gps = new ArrayList<Byte>();

        File image_with_gps = new File("Session1/image_with_gps_mine.jpeg");
        FileOutputStream img_gps_stream = new FileOutputStream(image_with_gps);

        do {
            k = modem.read();
            if (k == -1)
                break;

            if (k == 255) {
                last_k = k;
                k = modem.read();
                if (k == 216) {
                    image_gps_flag = true;

                    img_with_gps.add((byte) last_k);
                    img_with_gps.add((byte) k);
                }
            }
        } while (!image_gps_flag);

        for (;;) {
            k = modem.read();
            if (k == -1)
                break;
            img_with_gps.add((byte) k);
        }
        System.out.println("GPS with R at " + LocalDateTime.now());
        try {
            img_gps_stream.write(convertBytes(img_with_gps));
            img_gps_stream.close();

        } catch (Exception x) {
            System.out.println(x);
        }

    }

    public static void ARQerror(Modem modem) throws FileNotFoundException {
        byte[] ackreq = ACK_request_code.getBytes();

        File Output_from_arq = new File("Session1/Arq.txt");
        FileOutputStream arq_stream = new FileOutputStream(Output_from_arq);

        File Time_for_arq = new File("Session1/Time_arq.txt");
        FileOutputStream time_arq_stream = new FileOutputStream(Time_for_arq);

        long start, end;
        int k, ACK_pack = 0, all_pack = 0;
        int retry = 0;
        int[] NACK_pack_retry = new int[9];
        String text = "", all_text = "", response = "";
        long new_begin = System.currentTimeMillis();
        //For 5 minutes, check every package -until "PSTOP"-
        //if the XOR of the encrypted bytes is equal to FCS.
        //For every packages, measure the retries which is needed to achieve the equallity.
        //If the equality was achieved with the 1st attempt, give ACK request. Otherwise, give NACK request until the pairing.
        while (System.currentTimeMillis() - new_begin < 300000) {
            modem.write(ackreq);
            start = System.currentTimeMillis();
            text = "";
            for (;;) {
                k = modem.read();
                if (k != -1) {
                    text += (char) k;
                    if (text.indexOf("PSTOP") > -1) {
                        if (check_for_arq(text)) {
                            ACK_pack += 1;
                            end = System.currentTimeMillis();
                            response += Long.toString(end - start) + "\r\n";
                        } else {
                            retry = 0;
                            while (!check_for_arq(text)) {
                                text = "";
                                retry++;
                                modem.write(NACK_request_code.getBytes());
                                for (;;) {
                                    k = modem.read();
                                    if (k != -1) {
                                        text += (char) k;
                                        if (text.indexOf("PSTOP") > -1) {
                                            break;
                                        }
                                    } else
                                        break;

                                }

                            }
                            end = System.currentTimeMillis();
                            response += Long.toString(end - start) + "\r\n";
                            switch (retry)

                            {
                                case 1:
                                    NACK_pack_retry[0] += 1;
                                    break;
                                case 2:
                                    NACK_pack_retry[1] += 1;
                                    break;
                                case 3:
                                    NACK_pack_retry[2] += 1;
                                    break;
                                case 4:
                                    NACK_pack_retry[3] += 1;
                                    break;
                                case 5:
                                    NACK_pack_retry[4] += 1;
                                    break;
                                case 6:
                                    NACK_pack_retry[5] += 1;
                                    break;
                                case 7:
                                    NACK_pack_retry[6] += 1;
                                    break;
                                case 8:
                                    NACK_pack_retry[7] += 1;
                                    break;
                                case 9:
                                    NACK_pack_retry[8] += 1;
                                    break;
                                default:
                                    break;
                            }

                        }
                        all_pack += 1;
                        break;
                    }
                } else
                    break;

            }
            if (k == -1 && text == "")
                break;

        }
        int NACK_pack = 0;
        for (int i : NACK_pack_retry) {
            NACK_pack += i;
        }
        System.out.println("All packages " + all_pack);
        System.out.println("ACK packages " + ACK_pack);
        System.out.println("NACK packages " + NACK_pack);
        for (int i = 0; i < NACK_pack_retry.length; i++) {
            int index = i + 1;
            System.out.println(index + " retries " + " time " + NACK_pack_retry[i]);
        }
        System.out.println("ARQ Error at " + LocalDateTime.now());
        try {
            arq_stream.write(all_text.getBytes());
            arq_stream.close();
            time_arq_stream.write(response.getBytes());
            time_arq_stream.close();
        } catch (Exception x) {
            System.out.println(x);
        }

    }

    public static byte[] convertBytes(ArrayList<Byte> bytes) {
        byte[] ret = new byte[bytes.size()];
        Iterator<Byte> iterator = bytes.iterator();
        for (int i = 0; i < ret.length; i++) {
            ret[i] = iterator.next().byteValue();
        }
        return ret;
    }

    public static String convertCoordinates(double num) {
        // Build the number
        String final_string;
        int int_part;
        double fr_part;
        int sec;
        int_part = (int) num;
        fr_part = num - int_part;
        sec = (int) Math.round(fr_part * 60);
        final_string = String.valueOf(int_part) + String.valueOf(sec);

        return final_string;
    }

    public static boolean check_for_arq(String text) {
        /*
         * Inputs: The string which contains the package.
         * Outputs: Boolean, true if the XOR of the encrypted bytes its equal to FCS, false otherwise.
         * Find the sequence of the encrypted bytes and for every consecutively character find the XOR value.
         * Finally, find the FCS value of the input string and check the pairing.
         */
        String sequence;
        int FCS;

        sequence = text.substring(text.indexOf("<") + 1, text.indexOf(">"));
        int prev_c;
        int curr_c;
        prev_c = (int) sequence.charAt(0);
        for (int i = 1; i < sequence.length(); i++) {
            curr_c = (int) sequence.charAt(i);
            prev_c = (prev_c ^ curr_c);
        }
        FCS = Integer.parseInt(text.substring(text.indexOf(">") + 2, text.indexOf("PSTOP") - 1));
        return (FCS == prev_c);

    }

}