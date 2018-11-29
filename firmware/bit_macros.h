#ifndef BIT_MACROS_H
#define BIT_MACROS_H

/* Set bit in register */
#define		sbi(reg, bit)		reg |= (1<<bit)	

/* Clear bit in register */
#define		cbi(reg, bit)		reg &= (~(1<<bit))	

/* Check is set bit in register */
#define		issbi(reg, bit)		((reg & (1<<bit)) != 0)	

/* Check is clear bit in register */
#define		iscbi(reg, bit)		((reg & (1<<bit)) == 0)	

#endif //BIT_MACROS_H