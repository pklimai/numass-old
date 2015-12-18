/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package inr.numass.data;

import hep.dataforge.content.Named;
import hep.dataforge.meta.Meta;
import java.time.Instant;
import java.util.List;

/**
 *
 * @author Alexander Nozik <altavir@gmail.com>
 */
public interface NumassData extends Named{

    String getDescription();

    Meta getInfo();

    List<NMPoint> getNMPoints();

    boolean isEmpty();

    Instant startTime();

}