/**
 * HeftyInteger for CS1501 Project 5
 * @author	Dr. Farnan
 */
package cs1501_p5;

import java.math.BigInteger;

public class HeftyInteger {

	private final byte[] ONE = {(byte) 1};

	private final byte[] ZERO = {(byte) 0};

	private byte[] val;

	/**
	 * Construct the HeftyInteger from a given byte array
	 * @param b the byte array that this HeftyInteger should represent
	 */
	public HeftyInteger(byte[] b) {
		val = b;
	}

	/**
	 * Return this HeftyInteger's val
	 * @return val
	 */
	public byte[] getVal() {
		return val;
	}

	/**
	 * Return the number of bytes in val
	 * @return length of the val byte array
	 */
	public int length() {
		return val.length;
	}

	/**
	 * Add a new byte as the most significant in this
	 * @param extension the byte to place as most significant
	 */
	public void extend(byte extension) {
		byte[] newv = new byte[val.length + 1];
		newv[0] = extension;
		for (int i = 0; i < val.length; i++) {
			newv[i + 1] = val[i];
		}
		val = newv;
	}

	/**
	 * If this is negative, most significant bit will be 1 meaning most
	 * significant byte will be a negative signed number
	 * @return true if this is negative, false if positive
	 */
	public boolean isNegative() {
		return (val[0] < 0);
	}

	/**
	 * Computes the sum of this and other
	 * @param other the other HeftyInteger to sum with this
	 */
	public HeftyInteger add(HeftyInteger other) {
		byte[] a, b;
		// If operands are of different sizes, put larger first ...
		if (val.length < other.length()) {
			a = other.getVal();
			b = val;
		}
		else {
			a = val;
			b = other.getVal();
		}

		// ... and normalize size for convenience
		if (b.length < a.length) {
			int diff = a.length - b.length;

			byte pad = (byte) 0;
			if (b[0] < 0) {
				pad = (byte) 0xFF;
			}

			byte[] newb = new byte[a.length];
			for (int i = 0; i < diff; i++) {
				newb[i] = pad;
			}

			for (int i = 0; i < b.length; i++) {
				newb[i + diff] = b[i];
			}

			b = newb;
		}

		// Actually compute the add
		int carry = 0;
		byte[] res = new byte[a.length];
		for (int i = a.length - 1; i >= 0; i--) {
			// Be sure to bitmask so that cast of negative bytes does not
			//  introduce spurious 1 bits into result of cast
			carry = ((int) a[i] & 0xFF) + ((int) b[i] & 0xFF) + carry;

			// Assign to next byte
			res[i] = (byte) (carry & 0xFF);

			// Carry remainder over to next byte (always want to shift in 0s)
			carry = carry >>> 8;
		}

		HeftyInteger res_li = new HeftyInteger(res);

		// If both operands are positive, magnitude could increase as a result
		//  of addition
		if (!this.isNegative() && !other.isNegative()) {
			// If we have either a leftover carry value or we used the last
			//  bit in the most significant byte, we need to extend the result
			if (res_li.isNegative()) {
				res_li.extend((byte) carry);
			}
		}
		// Magnitude could also increase if both operands are negative
		else if (this.isNegative() && other.isNegative()) {
			if (!res_li.isNegative()) {
				res_li.extend((byte) 0xFF);
			}
		}

		// Note that result will always be the same size as biggest input
		//  (e.g., -127 + 128 will use 2 bytes to store the result value 1)
		return res_li;
	}

	/**
	 * Negate val using two's complement representation
	 * @return negation of this
	 */
	public HeftyInteger negate() {
		byte[] neg = new byte[val.length];
		int offset = 0;

		// Check to ensure we can represent negation in same length
		//  (e.g., -128 can be represented in 8 bits using two's
		//  complement, +128 requires 9)
		if (val[0] == (byte) 0x80) { // 0x80 is 10000000
			boolean needs_ex = true;
			for (int i = 1; i < val.length; i++) {
				if (val[i] != (byte) 0) {
					needs_ex = false;
					break;
				}
			}
			// if first byte is 0x80 and all others are 0, must extend
			if (needs_ex) {
				neg = new byte[val.length + 1];
				neg[0] = (byte) 0;
				offset = 1;
			}
		}

		// flip all bits
		for (int i  = 0; i < val.length; i++) {
			neg[i + offset] = (byte) ~val[i];
		}

		HeftyInteger neg_li = new HeftyInteger(neg);

		// add 1 to complete two's complement negation
		return neg_li.add(new HeftyInteger(ONE));
	}

	/**
	 * Implement subtraction as simply negation and addition
	 * @param other HeftyInteger to subtract from this
	 * @return difference of this and other
	 */
	public HeftyInteger subtract(HeftyInteger other) {
		return this.add(other.negate());
	}

	/**
	 * Compute the product of this and other
	 * @param other HeftyInteger to multiply by this
	 * @return product of this and other
	 */
	public HeftyInteger multiply(HeftyInteger other) {
		byte[] res = new byte[this.length() + other.length()];
		HeftyInteger result = new HeftyInteger(res);

		HeftyInteger a = new HeftyInteger(this.getVal());
		HeftyInteger b = new HeftyInteger(other.getVal());

		boolean negative = false;
		if(this.isNegative() && other.isNegative()){
			a = a.negate();
			b = b.negate();
		} else if (this.isNegative() && !other.isNegative()){
			negative = true;
			a = a.negate();
		} else if (!this.isNegative() && other.isNegative()){
			negative = true;
			b = b.negate();
		}

		result = a.mult(b);

		if (negative) result = result.negate();
		return result;
	}


	public HeftyInteger mult(HeftyInteger other){
		byte[] b = new byte[this.length() + other.length()];
		HeftyInteger result = new HeftyInteger(b);
		
		int shift = 0;
		int count = 1;
		for (int i = other.length()-1; i >= 0; i--){
			for (int j = this.length()-1; j >= 0; j--){
				byte[] n = new byte[2];
				n[0] = (byte) ((this.getVal()[j] & 0xFF)*(other.getVal()[i] & 0xFF) >> 8);
				n[1] = (byte) ((this.getVal()[j] & 0xFF)*(other.getVal()[i] & 0xFF));
				HeftyInteger curr = new HeftyInteger(n);
				curr = curr.shifter(shift++);
				result = result.add(curr);
			}
			shift = count++;

		}

		return result;
	}


	public HeftyInteger shifter(int val){
		byte[] n = new byte[this.length() + val];

		for (int i = 0; i < this.length(); i++){
			n[i] = this.getVal()[i];
		}
		for (int i = this.length(); i < n.length;i++){
			n[i] = (byte) 0;
		}
		return new HeftyInteger(n);
	}

	

	public HeftyInteger remainder(HeftyInteger other){
		HeftyInteger a = this;
		while (!a.subtract(other).isNegative()){
			a = a.subtract(other);
		}
		return a;
	}

	public HeftyInteger quotient(HeftyInteger other){
		byte[] count = new byte[1];
		HeftyInteger COUNT = new HeftyInteger(count);
		HeftyInteger a = this;
		while (!a.subtract(other).isNegative()){
			a = a.subtract(other);
			COUNT = COUNT.add(new HeftyInteger(ONE));
		}
		return COUNT;
	}

	public boolean equals(HeftyInteger other){
		if (this.length() != other.length()){
			for (int i = 0;i < this.length(); i++){
				if (this.getVal()[i] != (byte) 0) return false;
			}
			for (int i = 0;i < other.length(); i++){
				if (other.getVal()[i] != (byte) 0) return false;
			}
			return true;
		} else{
			for (int i = 0; i < this.length(); i++){
				if(this.getVal()[i] != other.getVal()[i]) return false;
			}
			return true;
		}
	}

	public HeftyInteger[] recXGCD(HeftyInteger a, HeftyInteger b){
		if (b.equals(new HeftyInteger(ZERO)))
			return new HeftyInteger[] {a, new HeftyInteger(ONE), new HeftyInteger(ZERO)};
		HeftyInteger[] gcd = recXGCD(b, a.remainder(b));
		return new HeftyInteger[] {gcd[0], gcd[2], gcd[1].subtract(a.quotient(b).multiply(gcd[2]))};
	}

	/**
	 * Run the extended Euclidean algorithm on this and other
	 * @param other another HeftyInteger
	 * @return an array structured as follows:
	 *   0:  the GCD of this and other
	 *   1:  a valid x value
	 *   2:  a valid y value
	 * such that this * x + other * y == GCD in index 0
	 */
	 public HeftyInteger[] XGCD(HeftyInteger other) {
		HeftyInteger a = this;
		HeftyInteger b = other;

		if (this.isNegative()){
			a = this.negate();
		}
		if (other.isNegative()){
			b = other.negate();
		}

		HeftyInteger[] xgcdVal = recXGCD(a, b);
		
		if (!this.multiply(xgcdVal[1]).add(other.multiply(xgcdVal[2])).isNegative()){
			return xgcdVal;
		} else{
			if (this.multiply(xgcdVal[1].negate()).add(other.multiply(xgcdVal[2])).isNegative()){
				xgcdVal[1] = xgcdVal[1].negate();
			}else if (this.multiply(xgcdVal[1]).add(other.multiply(xgcdVal[2].negate())).isNegative()){
				xgcdVal[2] = xgcdVal[2].negate();
			}else if (this.multiply(xgcdVal[1].negate()).add(other.multiply(xgcdVal[2].negate())).isNegative()){
				xgcdVal[1] = xgcdVal[1].negate();
				xgcdVal[2] = xgcdVal[2].negate();
			} 
			return xgcdVal;
		}
	 }

}
