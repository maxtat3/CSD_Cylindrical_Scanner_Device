// #include <avr/io.h>
// #include <stdbool.h>

#include "indication.h"


static uint8_t curr_blink_intv = 0;


void init_ind_led_msr(void){
	// Настриваем на ВЫХОД порт для подключения ind led 
	sbi(LED_MSR_DDR, LED_MSR);
}

void ind_led_msr_state(bool state, bool blink, uint8_t interval){ 
	// steady state, not blinked 
	if (!blink){
		if (state){
			sbi(LED_MSR_PORT, LED_MSR);
		} else {
			cbi(LED_MSR_PORT, LED_MSR);
		}
	}

	// blinking 
	if (state && blink && (++curr_blink_intv > interval)){ 
		LED_MSR_PORT ^= (1<<(LED_MSR));
		curr_blink_intv = 0;
	}
	// blinking (alternate variant)
	// if (state && blink){
	// 	if (curr_blink_intv > interval){
	// 		LED_MSR_PORT ^= (1<<(LED_MSR));
	// 		curr_blink_intv = 0;
	// 	} else {
	// 		curr_blink_intv++;
	// 	}
	// }
}
