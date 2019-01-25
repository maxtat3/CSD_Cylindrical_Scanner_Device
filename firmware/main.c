#include <avr/io.h>
#include <util/delay.h>
#include <avr/interrupt.h>
#include <util/atomic.h>
#include <stdbool.h>
#include "bit_macros.h"
#include "uart.h"
#include "adc.h"
#include "indication.h"
#include "opto_interrupter.h"


// ==========================================
// 			Шаговый двигатель
// ==========================================
/* Определение регистров порта к которму подключен шаговый двигатель (ШД) */
#define		SM_PORT		PORTD		
#define		SM_PIN		PIND
#define		SM_DDR		DDRD

/* Определения номера порта для включения DC-DC преобразователя - ИП для ШД */
#define		SM_EN	3

/* определения номеров портов к котрым подключены обмотки ШД */
#define		SM_WIRE_1	4
#define		SM_WIRE_2	5
#define		SM_WIRE_3	6
#define		SM_WIRE_4	7

// ==========================================
// 			Настройки ТС1, отвечающего за
//			вращение ШД
// ==========================================
/* Начальное значение регистра TCNT1, выбираеться в зависимости от скорости  вращения ШД */
#define 	TCNT1_SM_FORVARD_MOVE		64000 // срабатывание каждые ~ 13.3 ms
#define 	TCNT1_SM_REVERSE_MOVE		64380 // срабатывание каждые ~ 10.0 ms

/* Максимальное количество шагов в одном направлении. Зависит от длины максимального перемещения ротора в одном направлении */
// #define		SM_MAX_PROGREESS	300

// ==========================================
// 			Другие макросы
// ==========================================
/* Количество полных поворотов ротора ШД для перемещения каретки в червячной передачи на 1 см */
#define 	SM_FULL_ONE_ROTATE_1CM	9

/* Количество шагов ротора ШД при полном обороте на 360 град. (нормальный шаг)*/
#define 	SM_ONEROTATE_360_DEGR	95

/* Полная длина (см) на которую перемещаеться каретка, достаточная для полного измерения образца */
#define 	FULL_LENGTH_CM			6

/* Вычисление количества шагов ШД для перемещения каретки в червячной передачи на полную длину , при выполнении измерений образца */
#define		SM_FULL_MSR_STEPS		SM_FULL_ONE_ROTATE_1CM * SM_ONEROTATE_360_DEGR * FULL_LENGTH_CM


/* задержка между каждым шагом при врещении ШД, это определяет скорсть вращения. Используеться только для парковки в нач состояние ШД */
#define		SM_DELAY_STEP_MS	5

/* Задержка между данными и командой при отправке в COM порт */
// #define		USART_CMD_OR_DATA_DELAY	2	// not used ?


/* Выполнен ли захват ТС1, т.е. включен он (ШД вращаеться) или нет. false - ТС1 свободен (выключен), ШД остановлен; true - ТС1 включен, ШД выполняет вращение */
volatile bool isBlockTC1 = false;

// запоминае состояние состояния кнопки запуска/остановки процееса измерений
volatile bool btnStateFlag = false;




// таблица шагов ШД , нормальный шаг
// const char smTableNormalStep[] = {_BV(SM_WIRE_1), _BV(SM_WIRE_2), _BV(SM_WIRE_3), _BV(SM_WIRE_4)};
const int8_t smTableNormalStep[] = {_BV(SM_WIRE_4), _BV(SM_WIRE_3), _BV(SM_WIRE_2), _BV(SM_WIRE_1)};



/* Command codes */
/** Запуск измерений **/
#define		CMD_MAKING_MSR		1

/** Остановка измерений **/
#define		CMD_STOP_MSR		2

/** Выполняетс парковка в начальное положение **/
#define 	CMD_MAKING_PARKING	3

/** Парковка в начальное положение закончена **/
#define 	CMD_STOP_PARKING	4

/** Идентефикация этого устройства на выбранном COM порту **/
#define 	CMD_INIT_DEVICE		5
const char DATA_RIGHT_INIT_DEVICE[] = {'C', 'S', 'D'};


/* Перечислегние команд от ПК */
enum fromPcCommands{
	STDBY,
	DO_START_SM,
	DO_STOP_SM
};
volatile enum fromPcCommands pcCommand = STDBY;



static uint8_t rxBuff[RX_BUFF_SIZE];
static uint8_t cmd = 0;
static uint32_t data = 0;

static uint8_t txPckBuff[TX_BUFF_SIZE];


/** Формирование RSP пакта с 1-м Байтом данных для отправки на сервер **/
void codingRspPck8(uint8_t data, uint8_t cmd);

/** Формирование RSP пакта с 2-я Байтами данных для отправки на сервер **/
void codingRspPck16(uint16_t data, uint8_t cmd);

/** Формирование RSP пакта с 3-я Байтами данных для отправки на сервер **/
void codingRspPck32(uint32_t data, uint8_t cmd);

void initIO(void);


void initExtInt0(void);
void initTC2(void);
// void initTC0(void);	// todo - not used ? 
void turnOnTC1(void);
void turnOffTC1(void);

bool checkSMInBeginPos(void);
void stopSM(void);


int main(void){
	cli();
	initIO();
	initUSART();
	initADC();
	// initExtInt0();
	// initTC2();
	init_ind_led_msr();
	init_opto_interrupter();
	sei();

	checkSMInBeginPos();

	start_cont_conv();


	while(1){
		getPckOfUART(rxBuff);

		cmd = (uint8_t)rxBuff[0] & 0xFF;

		if (cmd == CMD_INIT_DEVICE){
			uint8_t rsp[TX_BUFF_SIZE];
			rsp[0] = CMD_INIT_DEVICE;
			for (uint8_t i = 1; i < TX_BUFF_SIZE; ++i){
				rsp[i] = DATA_RIGHT_INIT_DEVICE[i-1];
			}
			sendPckToUART(rsp);

		} else if (cmd == CMD_MAKING_MSR){
			pcCommand = DO_START_SM;

		} else if(cmd == CMD_STOP_MSR){
			pcCommand = DO_STOP_SM;

		}


		switch(pcCommand){
			case STDBY:
				break;

			case DO_START_SM:
				if (isBlockTC1 == false){
					cli();
					turnOnTC1();
					sei();
				}
				break;

			case DO_STOP_SM:
				btnStateFlag = false;
				break;
		}

	}
}




// Обработчик кнопка для запуска/остановки процееса измерений
ISR(INT0_vect){
	// start measuring
	if (btnStateFlag == false){
		btnStateFlag = true;
		pcCommand = DO_START_SM;
	// stop measuring
	} else if (btnStateFlag == true){
		btnStateFlag = false;
		pcCommand = DO_STOP_SM;
	}

	_delay_ms(200); // антидребизг
}


// константа определяющая количество шагов которое нужно пропустить 
// прежде чем отправить значение ацп с датчика освещенности.
// Это нужно для синхронизации количества шагов с еденицами измерения длины образца в мм.
// 95 * 9 = 855 - перемещение каретки на 1 см.  См. SM_FULL_ONE_ROTATE_1CM SM_ONEROTATE_360_DEGR
// Округлим это значение до 850, т.к. полученные данные приблеженные. 
// Далее 10 мм делим на 0.2 мм = 50 раз. В 50 раз нужно уменьшить полученное число шагов. 
// Т.е. 850 делим на 50 = 17 шагов нужно для перемещения червяка на 0.2 мм. 
// Это приблеженное значение т.к. SM_FULL_ONE_ROTATE_1CM SM_ONEROTATE_360_DEGR требуют калибровки!
const uint8_t stepAdcSyncConst = 16;

ISR(TIMER1_OVF_vect){
	// в каком направлении выполняеться движение ШД
	// false - прямое ; true - обратное
	static bool isDisableForward = false;
	// счетчик состяний обмоток ШД
	static int8_t stepCount = 0;
	// счетчик шагов ШД в процессе измерения
	static int16_t smProgressCount = 0;
	static uint8_t stepAdcSyncCount = 0;

	// прямой ход ШД - выполнение измерений
	if(smProgressCount < SM_FULL_MSR_STEPS && !isDisableForward){
		if (pcCommand == DO_START_SM){
			ind_led_msr_state(true, false, 0);

			SM_PORT = smTableNormalStep[stepCount] | _BV(SM_EN);
			stepCount ++;
			if (stepCount > 3) stepCount = 0;

			if (stepAdcSyncCount == stepAdcSyncConst){
				codingRspPck8((uint8_t)(get_adc_res()/4), CMD_MAKING_MSR);
				sendPckToUART(txPckBuff);
				stepAdcSyncCount = 0;
			} else {
				stepAdcSyncCount ++;
			}

			smProgressCount ++;

		}else if (pcCommand == DO_STOP_SM){
			isDisableForward = true;
			stepCount = 3;
		}
	}else if(!isDisableForward){
		isDisableForward = true;
		stepCount = 3;
		_delay_ms(300);
	}

	// обратный ход ШД - возвращение в исходное положение
	if (smProgressCount > 0 && isDisableForward){
		SM_PORT = smTableNormalStep[stepCount] | _BV(SM_EN);
		stepCount --;
		if (stepCount < 0) stepCount = 3;

		smProgressCount --;

		ind_led_msr_state(true, true, LED_BLINK_MEDIUM);
	}

	// останока ШД
	if (smProgressCount == 0 && isDisableForward){
		isDisableForward = false;
		stepCount = 0;
		smProgressCount = 0;
		turnOffTC1();
		ind_led_msr_state(false, false, 0);
	}

	if (!isDisableForward){
		TCNT1 = TCNT1_SM_FORVARD_MOVE;
	} else {
		TCNT1 = TCNT1_SM_REVERSE_MOVE;
	}
}



void codingRspPck8(uint8_t data, uint8_t cmd) {
	*(txPckBuff) = cmd & 0xFF;
	*(txPckBuff+1) = data & 0xFF;
	*(txPckBuff+2) = 0;
	*(txPckBuff+3) = 0;
}

void codingRspPck16(uint16_t data, uint8_t cmd) {
	*(txPckBuff) = cmd & 0xFF;
	*(txPckBuff+1) = data & 0xFF;
	*(txPckBuff+2) = (data >> 8) & 0xFF;
	*(txPckBuff+3) = 0;
}

void codingRspPck32(uint32_t data, uint8_t cmd) {
	*(txPckBuff) = cmd & 0xFF;
	*(txPckBuff+1) = data & 0xFF;
	*(txPckBuff+2) = (data >> 8) & 0xFF;
	*(txPckBuff+3) = (data >> 16) & 0xFF;
}


/* инициализация портов в/в */
void initIO(void){
	/* Настприваем на ВЫХОД порт к которым подклчен ИП для ШД */
	sbi(SM_DDR, SM_EN);

	/* Настприваем на ВЫХОД порты к которым подклчен двигатель */
	sbi(SM_DDR, SM_WIRE_1);
	sbi(SM_DDR, SM_WIRE_2);
	sbi(SM_DDR, SM_WIRE_3);
	sbi(SM_DDR, SM_WIRE_4);

	stopSM();

	/* Настриваем на ВЫХОД порт для меандра  */
	// sbi(MEANDER_DDR, MEANDER); 
}


// настройка прерывания от внешнего источник (кнопки)
void initExtInt0(){
	// срабатывание по низкому уровню на выводе INT0
	cbi(MCUCR, ISC00);
	cbi(MCUCR, ISC01);
	// резрещаем внешние прерывания
	sbi(GICR, INT0);
}


// Запуск ТС1 
void turnOnTC1(){
	isBlockTC1 = true;
	// разрешаем прерывания при переполнении
	sbi(TIMSK, TOIE1);

	// устанавливаем делитель частоты 64
	sbi(TCCR1B, CS10);
	sbi(TCCR1B, CS11);
	cbi(TCCR1B, CS12);
	
	// устанавливаем нормальный режим
	// в этом режиме OCR1A нельзя изменить, OCR1A = 0xFFFF
	cbi(TCCR1A, WGM10);
	cbi(TCCR1A, WGM11);
	cbi(TCCR1B, WGM12);
	cbi(TCCR1B, WGM13);

	// подстраиваим частоту срабатываения
	TCNT1 = TCNT1_SM_FORVARD_MOVE;
}

//Отключаем ТС1
void turnOffTC1(){
	cli();
	// выключение таймера
	cbi(TCCR1B, CS10);
	cbi(TCCR1B, CS11);
	cbi(TCCR1B, CS12);

	stopSM();

	pcCommand = DO_STOP_SM;
	isBlockTC1 = false;
	sei();
}


// Проверка расположения (парковки) ШД в исходном (начальном) положении 
// Обратный ход ШД до закрытия окна опто-прерывателя
bool checkSMInBeginPos(){
	cli();

	while (opto_intrpt_is_closed()){
		for (int8_t i = 3; i >= 0; --i){
			SM_PORT = smTableNormalStep[i] | _BV(SM_EN);
			_delay_ms(SM_DELAY_STEP_MS);
			ind_led_msr_state(true, true, LED_BLINK_MEDIUM);
			if (opto_intrpt_is_closed()) continue;
		}
	}

	stopSM();
	ind_led_msr_state(false, false, 0);
	sei();
	return true;
}

// остановка ШД - снятия напряжения с управляющих выводов
// отключение ИП для ШД
void stopSM(){
	cbi(SM_PORT, SM_EN);
	cbi(SM_PORT, SM_WIRE_1);
	cbi(SM_PORT, SM_WIRE_2);
	cbi(SM_PORT, SM_WIRE_3);
	cbi(SM_PORT, SM_WIRE_4);
}
