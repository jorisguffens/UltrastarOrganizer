/*
 * This file is part of Ultrastar Organizer, licensed under the MIT License.
 *
 * Copyright (c) Joris Guffens
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package be.jorisg.ultrastarorganizer.commands;

import be.jorisg.ultrastarorganizer.entity.SongInfo;
import be.jorisg.ultrastarorganizer.utils.Utils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.odftoolkit.odfdom.doc.OdfTextDocument;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.odftoolkit.odfdom.doc.table.OdfTableCell;
import org.odftoolkit.odfdom.dom.element.office.OfficeTextElement;
import org.odftoolkit.odfdom.dom.element.style.StyleMasterPageElement;
import org.odftoolkit.odfdom.dom.style.OdfStyleFamily;
import org.odftoolkit.odfdom.dom.style.props.*;
import org.odftoolkit.odfdom.incubator.doc.office.OdfOfficeStyles;
import org.odftoolkit.odfdom.incubator.doc.style.OdfStyle;
import org.odftoolkit.odfdom.incubator.doc.style.OdfStylePageLayout;
import org.odftoolkit.odfdom.incubator.doc.text.OdfTextHeading;
import org.w3c.dom.Node;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "songlist",
        description = "Create a document with a list of all songs.")
public class SongList implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", description = "The ultrastar library.")
    private File directory;

    @Override
    public Integer call() throws Exception {

        System.out.println("Detecting songs...");

        List<SongInfo> songInfos = new ArrayList<>();
        for (File songDir : directory.listFiles()) {
            if (!songDir.isDirectory()) {
                continue;
            }

            try {
                SongInfo info = Utils.getMainInfoFile(songDir);
                songInfos.add(info);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        System.out.println(songInfos.size() + " songs will be processed.");

        // GENERATE ADMINISTRATIVE CSV
        File csvOutputFile = new File(FileSystems.getDefault().getPath(".").toFile(), "songlist.csv");
        if (!csvOutputFile.exists()) {
            csvOutputFile.createNewFile();
        }

        try (
                PrintWriter pw = new PrintWriter(csvOutputFile);
        ) {
            pw.write("SEP=,\n");

            CSVPrinter printer = new CSVPrinter(pw, CSVFormat.DEFAULT.withHeader(
                    "Artist", "Title", "Cover", "Background", "Video"));

            for (SongInfo info : songInfos) {
                printer.printRecord(info.getArtist(), info.getTitle(),
                        info.getCover() != null, info.getBackground() != null, info.getVideo() != null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Generated csv song list.");

        // GENERATE STYLED DOCUMENT
        OdfTextDocument odt = OdfTextDocument.newTextDocument();

        OfficeTextElement root = odt.getContentRoot();
        OdfOfficeStyles styles = odt.getOrCreateDocumentStyles();

        // Page layout
        StyleMasterPageElement defaultPage = odt.getOfficeMasterStyles().getMasterPage("Standard");
        String pageLayoutName = defaultPage.getStylePageLayoutNameAttribute();
        OdfStylePageLayout pageLayoutStyle = defaultPage.getAutomaticStyles().getPageLayout(pageLayoutName);
        pageLayoutStyle.setProperty(OdfPageLayoutProperties.Padding, "0");
        pageLayoutStyle.setProperty(OdfPageLayoutProperties.MarginTop, "1.3cm");
        pageLayoutStyle.setProperty(OdfPageLayoutProperties.MarginRight, "1.3cm");
        pageLayoutStyle.setProperty(OdfPageLayoutProperties.MarginBottom, "1.3cm");
        pageLayoutStyle.setProperty(OdfPageLayoutProperties.MarginLeft, "1.3cm");

        // cleanup
        Node childNode;
        childNode = root.getFirstChild();
        while (childNode != null) {
            root.removeChild(childNode);
            childNode = root.getFirstChild();
        }

        styles.getDefaultStyle(OdfStyleFamily.Paragraph).setProperty(OdfTextProperties.FontFamily, "Arial");
        styles.getDefaultStyle(OdfStyleFamily.Table).setProperty(OdfTextProperties.FontFamily, "Arial");

        // create heading style
        OdfStyle titleStyle = styles.newStyle("Title", OdfStyleFamily.Paragraph);
        titleStyle.setStyleDisplayNameAttribute("Title");
        titleStyle.setProperty(OdfTextProperties.FontFamily, "Arial");
        titleStyle.setProperty(OdfTextProperties.FontSize, "28pt");
        titleStyle.setProperty(OdfParagraphProperties.TextAlign, "center");

        // create title
        OdfTextHeading title = new OdfTextHeading(odt.getContentDom());
        title.addStyledContent("Title", "Ultrastar");
        root.appendChild(title);

        odt.newParagraph();
        odt.newParagraph();

        // create table style
        OdfStyle tableStyle = styles.newStyle("Table", OdfStyleFamily.Table);
        tableStyle.setStyleDisplayNameAttribute("Table");
        tableStyle.setProperty(OdfTableProperties.Align, "center");

        // create table
        OdfTable table = OdfTable.newTable(odt, songInfos.size(), 3);
        table.getOdfElement().setStyleName("Table");

        /*
        // create table header style
        OdfStyle tableHeaderStyle = styles.newStyle("Table_Header", OdfStyleFamily.TableCell);
        tableHeaderStyle.setStyleDisplayNameAttribute("Table Header");
        tableHeaderStyle.setProperty(OdfTableCellProperties.Border, "none");
        tableHeaderStyle.setProperty(OdfTextProperties.FontWeight, "bold");
        tableHeaderStyle.setProperty(OdfTextProperties.FontFamily, "Arial");
        tableHeaderStyle.setProperty(OdfParagraphProperties.TextAlign, "center");

        // table titles
        table.getCellByPosition(0, 0).getOdfElement().setStyleName("Table_Header");

        OdfTableCell col1 = table.getCellByPosition(1, 0);
        col1.setStringValue("ARTIST");
        col1.getOdfElement().setStyleName("Table_Header");

        OdfTableCell col2 = table.getCellByPosition(2, 0);
        col2.setStringValue("TITLE");
        col2.getOdfElement().setStyleName("Table_Header");
         */

        // create table cell style
        OdfStyle tableCellStyle = styles.newStyle("Table_Cell", OdfStyleFamily.TableCell);
        tableCellStyle.setStyleDisplayNameAttribute("Table Cell");
        tableCellStyle.setProperty(OdfTableCellProperties.Border, "none");
        tableCellStyle.setProperty(OdfTextProperties.FontFamily, "Arial");
        tableCellStyle.setProperty(OdfTextProperties.FontSize, "10pt");
        tableCellStyle.setProperty(OdfTableCellProperties.PaddingTop, "2pt");
        tableCellStyle.setProperty(OdfTableCellProperties.PaddingBottom, "2pt");
        tableCellStyle.setProperty(OdfTableCellProperties.PaddingLeft, "10pt");
        tableCellStyle.setProperty(OdfTableCellProperties.PaddingRight, "10pt");

        // create odd table row style
        OdfStyle tableOddRowStyle = styles.newStyle("Table_Odd_Row", OdfStyleFamily.TableRow);
        tableOddRowStyle.setStyleDisplayNameAttribute("Table Odd Row");
        tableOddRowStyle.setProperty(OdfTableRowProperties.BackgroundColor, "#f6f6f6");

        // table contents
        for (int i = 0; i < songInfos.size(); i++) {
            SongInfo si = songInfos.get(i);
            table.getCellByPosition(0, i).setStringValue((i + 1) + "");
            table.getCellByPosition(1, i).setStringValue(si.getArtist());
            table.getCellByPosition(2, i).setStringValue(si.getTitle());

            for ( int j = 0; j < table.getColumnCount(); j++ ) {
                table.getCellByPosition(j, i).getOdfElement().setStyleName("Table_Cell");
            }

            if ( i % 2 == 1 ) {
                table.getRowByIndex(i).getOdfElement().setStyleName("Table_Odd_Row");
            }
        }

        // save document to file
        File docOutputFile = new File(FileSystems.getDefault().getPath(".").toFile(), "songlist.odt");
        odt.save(docOutputFile);

        System.out.println("Generated document song list.");

        return 0;
    }
}
