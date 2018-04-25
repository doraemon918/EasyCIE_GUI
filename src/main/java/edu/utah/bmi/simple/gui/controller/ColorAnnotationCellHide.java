package edu.utah.bmi.simple.gui.controller;

import edu.utah.bmi.nlp.sql.DAO;
import edu.utah.bmi.nlp.sql.RecordRow;
import edu.utah.bmi.nlp.sql.RecordRowIterator;
import edu.utah.bmi.simple.gui.entry.StaticVariables;
import edu.utah.bmi.simple.gui.entry.TaskFX;
import edu.utah.bmi.simple.gui.task.ConfigKeys;
import edu.utah.bmi.simple.gui.task.ViewOutputDB;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.io.File;
import java.sql.SQLException;

import static edu.utah.bmi.simple.gui.entry.StaticVariables.snippetLength;


/**
 * @author Jianlin Shi
 * Created on 2/13/17.
 */
public class ColorAnnotationCellHide extends ColorAnnotationCell {
    protected Color[] colors = new Color[]{Color.LIGHTGREY, Color.WHITE};
    protected Background[] backgrounds = new Background[]{new Background(new BackgroundFill(colors[0], null, null)),
            new Background(new BackgroundFill(colors[1], null, null))};
    protected static String previousValue = "";
    protected static int backgroundId = 0;
    private static String previousHTML="";


    protected void updateItem(Object item, boolean empty) {
//        System.out.println(">>" + item + "<<");
        superUpdateItem(item, empty);
        if (!empty) {
            hbox = new HBox();
            if (item instanceof RecordRow)
                addText(((RecordRow) item).getStrByColumnName("DOC_NAME"));
            else
                addText(item + "");
        }
    }

    protected void addText(String text) {
        if (!text.equals(previousValue)) {
            backgroundId = backgroundId == 1 ? 0 : 1;
            previousValue = text;
        }
        renderHighlighter("", "", text, "000000", backgrounds[backgroundId]);
    }


    public String generateHTML() {
//       when click DOC_NAME, show document text with snippet highlighted
        String html;
        Object value = this.itemProperty().getValue();
        if (value instanceof RecordRow && ((RecordRow) value).getValueByColumnName("DOC_TEXT") != null) {
            RecordRow recordRow = (RecordRow) value;
            if (recordRow.getValueByColumnName("SNIPPET_BEGIN") == null ||
                    recordRow.getValueByColumnName("SNIPPET") == null ||
                    recordRow.getValueByColumnName("SNIPPET").equals("")) {
                html = recordRow.getStrByColumnName("DOC_TEXT");
                html = html.replaceAll("\\n", "<br>");
            } else {
                html = recordRow.getStrByColumnName("DOC_TEXT");
                String color = ColorAnnotationCell.pickColor(recordRow, ColorAnnotationCell.colorDifferential);
                int begin = (int) recordRow.getValueByColumnName("SNIPPET_BEGIN");
                int end = begin + recordRow.getStrByColumnName("SNIPPET").length();
                html = ColorAnnotationCell.generateHTML(html,
                        begin, end,
                        color);
            }
        } else if (value instanceof RecordRow) {
            RecordRow recordRow = (RecordRow) value;
            Object docName = recordRow.getValueByColumnName("DOC_NAME");
            if (!docName.equals(previousValue)) {
                html = "";
                TaskFX task = TasksOverviewController.currentTasksOverviewController.mainApp.tasks.getTask("settings");
                String inputDB = task.getValue(ConfigKeys.readDBConfigFileName);
                DAO dao = new DAO(new File(inputDB));
                String docTableName = task.getValue(ConfigKeys.inputTableName);
                dao.initiateTableFromTemplate("DOCUMENTS_TABLE", docTableName, false);
                RecordRowIterator records = dao.queryRecordsFromPstmt(docTableName, docName);
                if (records.hasNext()) {
                    html = records.next().getStrByColumnName("TEXT");
                }
                dao.close();
                if (html.length() > 0 && recordRow.getValueByColumnName("SNIPPET_BEGIN") != null) {
                    String color = ColorAnnotationCell.pickColor(recordRow, ColorAnnotationCell.colorDifferential);
                    int begin = (int) recordRow.getValueByColumnName("SNIPPET_BEGIN");
                    int end = begin + recordRow.getStrByColumnName("SNIPPET").length();
                    html = ColorAnnotationCell.generateHTML(html,
                            begin, end,
                            color);
                } else {
                    html = docName + "";
                }
                html = html.replaceAll("\\n", "<br>");
                previousHTML=html;
                previousValue=docName+"";
            }else{
                html=previousHTML;
            }
        } else {
            html = value + "";
            html = html.replaceAll("\\n", "<br>");
        }
        return html;

    }
}