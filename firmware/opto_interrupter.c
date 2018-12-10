#include "opto_interrupter.h"


void init_opto_interrupter(void){
	cbi(OPTO_INTRPT_DDR, OPTO_INTRPT);
}

bool opto_intrpt_is_closed(void){
	return iscbi(OPTO_INTRPT_PIN, OPTO_INTRPT);
}
