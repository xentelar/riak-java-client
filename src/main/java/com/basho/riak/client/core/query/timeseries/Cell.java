package com.basho.riak.client.core.query.timeseries;

import com.basho.riak.client.core.util.BinaryValue;
import com.basho.riak.client.core.util.CharsetUtils;
import com.basho.riak.protobuf.RiakTsPB;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;

/**
 * Holds a piece of data for a Time Series @{link Row}.
 * A cell can hold 5 different types of raw data:
 * <ol>
 * <li><b>Varchar</b>s, which can hold byte arrays. Commonly used to store encoded strings.</li>
 * <li><b>SInt64</b>s, which can hold any signed 64-bit integers.</li>
 * <li><b>Double</b>s, which can hold any 64-bit floating point numbers.</li>
 * <li><b>Timestamp</b>s, which can hold any unix/epoch timestamp. Millisecond resolution is required.</li>
 * <li><b>Boolean</b>s, which can hold a true/false value. </li>
 * </ol>
 * Immutable once created.
 *
 * @author Alex Moore <amoore at basho dot com>
 * @author Sergey Galkin <srggal at gmail dot com>
 * @since 2.0.3
 */

public class Cell
{
    private String varcharValue = "";
    private long sint64Value = 0L;
    private double doubleValue = 0.0;
    private long timestampValue = 0L;
    private boolean booleanValue = false;
    private int typeBitfield = 0;

    /**
     * Creates a new "Varchar" Cell, based on the UTF8 binary encoding of the provided String.
     *
     * @param varcharValue The string to encode and store.
     */
    public Cell(String varcharValue)
    {
        if (varcharValue == null)
        {
            throw new IllegalArgumentException("String value cannot be NULL.");
        }

        initVarchar(varcharValue);
    }

    /**
     * Creates a new "Varchar" cell from the provided BinaryValue.
     *
     * @param varcharValue The BinaryValue to store.
     */
    public Cell(BinaryValue varcharValue)
    {
        if (varcharValue == null)
        {
            throw new IllegalArgumentException("BinaryValue value cannot be NULL.");
        }

        initVarchar(varcharValue.toStringUtf8());

    }

    /**
     * Creates a new "Integer" Cell from the provided long.
     *
     * @param sint64Value The long to store.
     */
    public Cell(long sint64Value)
    {
        initSInt64(sint64Value);
    }

    /**
     * Creates a new double cell.
     *
     * @param doubleValue The double to store.
     */
    public Cell(double doubleValue)
    {
        initDouble(doubleValue);
    }

    /**
     * Creates a new "Boolean" Cell from the provided boolean.
     *
     * @param booleanValue The boolean to store.
     */
    public Cell(boolean booleanValue)
    {
        initBoolean(booleanValue);
    }

    /**
     * Creates a new "Timestamp" Cell from the provided Calendar, by fetching the current time in milliseconds.
     *
     * @param timestampValue The Calendar to fetch the timestamp from.
     */
    public Cell(Calendar timestampValue)
    {
        if (timestampValue == null)
        {
            throw new IllegalArgumentException("Calendar object for timestamp value cannot be NULL.");
        }

        initTimestamp(timestampValue.getTimeInMillis());
    }

    /**
     * Creates a new "Timestamp" Cell from the provided Date, by fetching the current time in milliseconds.
     *
     * @param timestampValue The Date to fetch the timestamp from.
     */
    public Cell(Date timestampValue)
    {
        if (timestampValue == null)
        {
            throw new IllegalArgumentException("Date object for timestamp value cannot be NULL.");
        }

        initTimestamp(timestampValue.getTime());
    }

    Cell(RiakTsPB.TsCell pbCell)
    {
        if (pbCell.hasBooleanValue())
        {
            initBoolean(pbCell.getBooleanValue());
        }
        else if (pbCell.hasDoubleValue())
        {
            initDouble(pbCell.getDoubleValue());
        }
        else if (pbCell.hasSint64Value())
        {
            initSInt64(pbCell.getSint64Value());
        }
        else if (pbCell.hasTimestampValue())
        {
            initTimestamp(pbCell.getTimestampValue());
        }
        else if(pbCell.hasVarcharValue())
        {
            initVarchar(pbCell.getVarcharValue().toStringUtf8());
        }
        else
        {
            throw new IllegalArgumentException("Unknown PB Cell encountered.");
        }
    }

    private Cell()
    {
    }

    private void initBoolean(boolean booleanValue)
    {
        typeBitfield |= 0x00000010;
        this.booleanValue = booleanValue;
    }

    private void initTimestamp(long timestampValue)
    {
        typeBitfield |= 0x00000008;
        this.timestampValue = timestampValue;
    }

    private void initDouble(double doubleValue)
    {
        typeBitfield |= 0x00000004;
        this.doubleValue = doubleValue;
    }

    private void initSInt64(long longValue)
    {
        typeBitfield |= 0x00000002;
        this.sint64Value = longValue;
    }

    private void initVarchar(String stringValue)
    {
        typeBitfield |= 0x00000001;
        this.varcharValue = stringValue;
    }

    /**
     * Creates a new "Timestamp" cell from the provided raw value.
     *
     * @param rawTimestampValue The epoch timestamp, including milliseconds.
     * @return The new timestamp Cell.
     */
    public static Cell newTimestamp(long rawTimestampValue)
    {
        final Cell cell = new Cell();
        cell.initTimestamp(rawTimestampValue);
        return cell;
    }

    public boolean hasVarcharValue()
    {
        return ((typeBitfield & 0x00000001) == 0x00000001);
    }

    public boolean hasLong()
    {
        return ((typeBitfield & 0x00000002) == 0x00000002);
    }

    public boolean hasDouble()
    {
        return ((typeBitfield & 0x00000004) == 0x00000004);
    }

    public boolean hasTimestamp()
    {
        return ((typeBitfield & 0x00000008) == 0x00000008);
    }

    public boolean hasBoolean()
    {
        return ((typeBitfield & 0x00000010) == 0x00000010);
    }

    public String getVarcharAsUTF8String()
    {
        return varcharValue;
    }

    public BinaryValue getVarcharValue()
    {
        return BinaryValue.unsafeCreate(varcharValue.getBytes(CharsetUtils.UTF_8));
    }

    public long getLong()
    {
        return sint64Value;
    }

    public double getDouble()
    {
        return doubleValue;
    }

    public long getTimestamp()
    {
        return timestampValue;
    }

    public boolean getBoolean()
    {
        return booleanValue;
    }

    public RiakTsPB.TsCell getPbCell()
    {
        final RiakTsPB.TsCell.Builder builder = RiakTsPB.TsCell.newBuilder();

        if (hasVarcharValue())
        {
            builder.setVarcharValue(ByteString.copyFromUtf8(varcharValue));
        }
        if (hasLong())
        {
            builder.setSint64Value(sint64Value);
        }
        if (hasTimestamp())
        {
            builder.setTimestampValue(timestampValue);
        }
        if (hasBoolean())
        {
            builder.setBooleanValue(booleanValue);
        }
        if (hasDouble())
        {
            builder.setDoubleValue(doubleValue);
        }

        return builder.build();
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("Cell{ ");

        if (this.hasVarcharValue())
        {
            final String value = this.getVarcharAsUTF8String();
            if (value.length() > 32)
            {
                sb.append(value.substring(0, 32));
                sb.append("...");
            }
            else
            {
                sb.append(value);
            }
        }
        else if (this.hasLong())
        {
            sb.append(this.getLong());
        }
        else if (this.hasDouble())
        {
            sb.append(this.getDouble());
        }
        else if (this.hasTimestamp())
        {
            sb.append(this.getTimestamp());
        }
        else if (this.hasBoolean())
        {
            sb.append(this.getBoolean());
        }

        sb.append(" }");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        Cell cell = (Cell) o;

        if (sint64Value != cell.sint64Value)
        {
            return false;
        }
        if (Double.compare(cell.doubleValue, doubleValue) != 0)
        {
            return false;
        }
        if (timestampValue != cell.timestampValue)
        {
            return false;
        }
        if (booleanValue != cell.booleanValue)
        {
            return false;
        }
        if (typeBitfield != cell.typeBitfield)
        {
            return false;
        }
        return varcharValue.equals(cell.varcharValue);

    }

    @Override
    public int hashCode()
    {
        int result;
        long temp;
        result = varcharValue.hashCode();
        result = 31 * result + (int) (sint64Value ^ (sint64Value >>> 32));
        temp = Double.doubleToLongBits(doubleValue);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (int) (timestampValue ^ (timestampValue >>> 32));
        result = 31 * result + (booleanValue ? 1 : 0);
        result = 31 * result + typeBitfield;
        return result;
    }
}
