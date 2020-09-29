package com.myfiziq.sdk.adapters;

/**
 * Defines layout options for the <code>RecyclerManager</code>.
 */
public enum LayoutStyle
{
    HORIZONTAL(1, 1),
    VERTICAL(1, 1),
    GRID_2(2, 2),
    GRID_3(3, 3);

    public final int mTabletColumns;
    public final int mPhoneColumns;

    LayoutStyle(int tabletColumns, int phoneColumns)
    {
        mTabletColumns = tabletColumns;
        mPhoneColumns = phoneColumns;
    }
}
