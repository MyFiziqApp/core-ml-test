#pragma version(1)
#pragma rs java_package_name(com.myfiziq.renderscript)

// set from the java SDK level
rs_allocation gIn;
rs_allocation gOut;

rs_script gScript;

void filter() {
    rsForEach(gScript, gIn, gOut, 0, 0);	// for each element of the input allocation,
    										// call root() method on gScript
}

void root(const uchar4 *v_in, uchar4 *v_out, const void *usrData, uint32_t x, uint32_t y) {
	//float4 f4 = rsUnpackColor8888(*v_in);	// extract RGBA values, see rs_core.rsh
    *v_out = *v_in;//rsPackColorTo8888(f3);
}
