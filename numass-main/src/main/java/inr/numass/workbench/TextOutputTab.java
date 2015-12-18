/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.workbench;

import hep.dataforge.fx.DataOutputPane;
import java.io.OutputStream;
import javafx.event.Event;

/**
 * A text output tab. Basically it is attached to IOManager::out
 * @author Alexander Nozik <altavir@gmail.com>
 */
public class TextOutputTab extends OutputTab {

    private final DataOutputPane out;

    /**
     * Create new stream output tab
     *
     * @param name
     * @param stream outputStream to which output should be redirected after
     * displaying in window
     */
    public TextOutputTab(String name) {
        super(name);
        out = new DataOutputPane();
        setContent(out);
        setOnClosed((Event event) -> close());
    }

    @Override
    public void clear() {
        out.clear();
    }

    @Override
    public final void close() {
        clear();
    }

    public OutputStream getStream(OutputStream forward) {
        return out.getOutputStream(forward);
    }

}