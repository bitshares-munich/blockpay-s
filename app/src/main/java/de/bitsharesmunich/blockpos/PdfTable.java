package de.bitsharesmunich.blockpos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import de.bitsharesmunich.database.HistoricalTransferEntry;
import de.bitsharesmunich.graphenej.AssetAmount;
import de.bitsharesmunich.graphenej.UserAccount;
import de.bitsharesmunich.graphenej.Util;
import de.bitsharesmunich.graphenej.operations.TransferOperation;
import de.bitsharesmunich.interfaces.ExportTaskListener;
import de.bitsharesmunich.utils.Helper;


/**
 * Created by developer on 5/23/16.
 */
public class PdfTable {
    private final String TAG = this.getClass().getName();

    private Context myContext;
    private String filename;
    private ExportTaskListener mListener;

    public PdfTable(Context context, String filename, ExportTaskListener listener) {
        this.myContext = context;
        this.filename = filename;
        this.mListener = listener;
    }

    public static String combinePath(String path1, String path2) {
        File file1 = new File(path1);
        File file2 = new File(file1, path2);
        return file2.getPath();
    }

    private void createEmptyFile(String path) {
        try {
            File gpxfile = new File(path);
            FileWriter writer = new FileWriter(gpxfile);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String createTable(Context context, List<HistoricalTransferEntry> myTransactions, UserAccount me) {
        Document document = new Document();
        try {
            String extStorage = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + myContext.getResources().getString(R.string.folder_name);
            final String filePath = combinePath(extStorage, filename + ".pdf");
            createEmptyFile(filePath);
            PdfWriter.getInstance(document, new FileOutputStream(filePath));

            document.open();

            PdfPTable table = new PdfPTable(4); // 3 columns.

            PdfPCell cell1 = new PdfPCell(new Paragraph(myContext.getString(R.string.date)));
            PdfPCell cell2 = new PdfPCell(new Paragraph(myContext.getString(R.string.sent_rcv)));
            PdfPCell cell3 = new PdfPCell(new Paragraph(myContext.getString(R.string.details)));
            PdfPCell cell4 = new PdfPCell(new Paragraph(myContext.getString(R.string.amount)));

            table.addCell(cell1);
            table.addCell(cell2);
            table.addCell(cell3);
            table.addCell(cell4);

            for (int i = 0; i < myTransactions.size(); i++) {
                table.completeRow();
                HistoricalTransferEntry transferEntry = myTransactions.get(i);
                TransferOperation operation = transferEntry.getHistoricalTransfer().getOperation();
                long timestamp = transferEntry.getTimestamp() * 1000;
                String time = Helper.convertDateToGMTWithYear(new Date(timestamp), context);
                String timeZone = Helper.convertTimeToGMT(new Date(timestamp), context);

                String dateText = String.format("%s\n%s", time, timeZone);
                PdfPCell dateCell = new PdfPCell(new Paragraph(dateText));
                table.addCell(dateCell);

                Drawable d;
                if (operation.getFrom().getObjectId().equals(me.getObjectId())) {
                    d = ContextCompat.getDrawable(myContext, R.drawable.sendicon);
                } else {
                    d = ContextCompat.getDrawable(myContext, R.drawable.rcvicon);
                }

                try {
                    Bitmap bitmap = ((BitmapDrawable) d).getBitmap();
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] imageInByte = stream.toByteArray();
                    Image myImage = Image.getInstance(imageInByte);
                    myImage.scalePercent(25);
                    PdfPCell sendReceiveCell = new PdfPCell(myImage, false);
                    sendReceiveCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    sendReceiveCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    table.addCell(sendReceiveCell);
                } catch (IOException ex) {
                    mListener.onError(ex.getMessage());
                    return ex.getMessage();
                }

                String from = myContext.getString(R.string.from_capital);
                String to = myContext.getString(R.string.to_capital);
                String memo = myContext.getString(R.string.memo_capital);

                String detailsText = String.format("" + to + ": %s\n" + from + ": %s\n" + memo + ": %s", operation.getTo().getName(), operation.getFrom().getName(), operation.getMemo().getPlaintextMessage());

                PdfPCell detailsCell = new PdfPCell(new Paragraph(detailsText));
                table.addCell(detailsCell);

                String amountText = "";
                AssetAmount assetAmount = operation.getAssetAmount();
                String preFormat = "%%.%df %%s";
                String format = String.format(preFormat, assetAmount.getAsset().getPrecision());
                if (operation.getFrom().getObjectId().equals(me.getObjectId())) {
                    amountText = "- " + String.format(format, Util.fromBase(assetAmount), assetAmount.getAsset().getSymbol());
                } else {
                    amountText = "+ " + String.format(format, Util.fromBase(assetAmount), assetAmount.getAsset().getSymbol());
                }
                PdfPCell amountsCell = new PdfPCell(new Paragraph(amountText));
                amountsCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(amountsCell);

                /* Updating progress */
                if (mListener != null && i % 8 == 0) {
                    float ratio = ((float) i) / ((float) myTransactions.size());
                    mListener.onUpdate(ratio);
                }
            }
            document.add(table);
            document.close();
            return myContext.getResources().getString(R.string.pdf_generated_msg) + filePath;
        } catch (Exception e) {
            Log.e(TAG, "Exception while trying to generate a PDF. Msg: " + e.getMessage());
            mListener.onError(e.getMessage());
            return "";
        }
    }
}