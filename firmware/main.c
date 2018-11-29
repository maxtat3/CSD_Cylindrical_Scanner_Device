#include <avr/io.h>
#include <util/delay.h>
#include <avr/interrupt.h>
#include <util/atomic.h>
#include <stdbool.h>
#include "bit_macros.h"
#include "uart.h"
#include "adc.h"


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
// 			Опто-прерыватель с открытым каналом 
// 			Датчик остановки шагового двигателя
// ==========================================
/* Определение регистров порта к которму подключен опто-прерыватель */
#define		OPTO_SENSOR_PORT	PORTB
#define		OPTO_SENSOR_DDR		DDRB
#define		OPTO_SENSOR_PIN		PINB

/* Номер порта к которому подключен опто-прерыватель*/
#define		OPTO_SENSOR		0

// ==========================================
// 			Led1 для логгирования
// ==========================================
/* определения регистров для logging led1 */
#define		STATE_LED_PORT		PORTB
#define		STATE_LED_DDR		DDRB

/* определения номера портв к которым подключен logging led1 */
#define 	STATE_LED 		1

// ==========================================
//			Вывод генерации меандра
// ==========================================
/* определения регистров */
#define		MEANDER_PORT		PORTB
#define		MEANDER_DDR			DDRB

/* определения номера порта на который выводиться меандр */
#define 	MEANDER 			2

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

//======================================
//	Команды от ПК
//	Каждая команда - массив символов
//======================================
int8_t pcToMcuStartMeasureComm[] = {'a', 'q', 'l'};
int8_t pcToMcuStopMeasureComm[] = {'a', 'b', 'k'};
int8_t pcToMcuInitDevice[] = {'a', 'g', 'd'};
int16_t commCount = 0; //общий счетчик для определения совпадения команды посимвольно 
/* Перечислегние команд от ПК */
enum fromPcCommands{
	STDBY,
	DO_START_SM,
	DO_STOP_SM
};
volatile enum fromPcCommands pcCommand = STDBY;

//======================================
//	Команды мк -> ПК
//	Каждая команда занимает только 1 Байт
//======================================
const int8_t startMsrCmd = 100; // выполняеться процесс измерений 



void initIO(void);


void initExtInt0(void);
void initTC2(void);
// void initTC0(void);	// todo - not used ? 
void turnOnTC1(void);
void turnOffTC1(void);

bool checkSMInBeginPos(void);
void stopSM(void);
void blinkLed1r(void);
void blinkLed2r(void);


int main(void){
	cli();
	initIO();
	initUSART();
	initADC();
	initExtInt0();
	initTC2();
	sei();

	checkSMInBeginPos();

	start_cont_conv();

	uint8_t sym;

	while(1){
		sym = getCharOfUSART();
		
		//=====================================
		//	Блоки проверки соответствия команд от ПК
		//=====================================
		if (sym == pcToMcuInitDevice[commCount]){
			commCount ++;
			if (commCount == sizeof(pcToMcuInitDevice)){
				sendDataToUSART('F'); //ascii = 70
				_delay_ms(35);
				sendDataToUSART('G'); //ascii = 71
				_delay_ms(35);
				sendDataToUSART('H'); //ascii = 72
				_delay_ms(35);
				commCount = 0;
			}
		} else 
		if (sym == pcToMcuStartMeasureComm[commCount]){
			commCount ++;
			if (commCount == sizeof(pcToMcuStartMeasureComm)){
				pcCommand = DO_START_SM;
				commCount = 0;
			}
		} else if (sym == pcToMcuStopMeasureComm[commCount]){
			commCount ++;
			if (commCount == sizeof(pcToMcuStopMeasureComm)){
				pcCommand = DO_STOP_SM;
				commCount = 0;
			}
		}

		//=========================================
		//	Блоки выполнения методов в зависимости
		//	от полученной команды от ПК
		//=========================================
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




// Множетель для получения больших значений времени срабатывания.
// Например при ocr2=195 -> t=25 ms. Соответственно для получения
// времени срабатывания в 1 s этот множетель = 40.
const uint8_t kSecFactor = 40;
// Счетчик множетелья.
volatile uint8_t kSecCount = 0;
// Переключение уровня сигнала с высокого в низкний
// и наоборот. Соответсвенно полученим меандр.
volatile bool isSwithOnMeandr = false;
ISR(TIMER2_COMP_vect){
	if (kSecCount == kSecFactor - 1){
		kSecCount = 0;
		if (!isSwithOnMeandr){
			sbi(MEANDER_PORT, MEANDER);
			isSwithOnMeandr = true;
		} else {
			cbi(MEANDER_PORT, MEANDER);
			isSwithOnMeandr = false;
		}
	} else {
		kSecCount ++;
	}
	
}

// константа определяющая количество шагов которое нужно пропустить 
// прежде чем отправить значение ацп с датчика освещенности.
// Это нужно для синхронизации количества шагов с еденицами измерения длины образца в мм.
// 95 * 9 = 855 - перемещение каретки на 1 см.  См. SM_FULL_ONE_ROTATE_1CM SM_ONEROTATE_360_DEGR
// Округлим это значение до 850, т.к. полученные данные приблеженные. 
// Далее 10 мм делим на 0.2 мм = 50 раз. В 50 раз нужно уменьшить полученное число шагов. 
// Т.е. 850 делим на 50 = 17 шагов нужно для перемещения червяка на 0.2 мм. 
// Это приблеженное значение т.к. SM_FULL_ONE_ROTATE_1CM SM_ONEROTATE_360_DEGR требуют калибровки!
const uint8_t stepAdcSyncConst = 17;

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

			SM_PORT = smTableNormalStep[stepCount] | _BV(SM_EN);
			stepCount ++;
			if (stepCount > 3) stepCount = 0;

			if (stepAdcSyncCount == stepAdcSyncConst){
				sendCmdAndDataToUSART(startMsrCmd, (uint8_t)(get_adc_res()/4));
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
	}

	// останока ШД
	if (smProgressCount == 0 && isDisableForward){
		isDisableForward = false;
		stepCount = 0;
		smProgressCount = 0;
		turnOffTC1();
	}

	if (!isDisableForward){
		TCNT1 = TCNT1_SM_FORVARD_MOVE;
	} else {
		TCNT1 = TCNT1_SM_REVERSE_MOVE;
	}
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

	/* Настриваем на ВХОД порт для подключения опто-прерывателя */
	cbi(OPTO_SENSOR_DDR, OPTO_SENSOR);
	
	/* Настриваем на ВЫХОД порт для подключения state led  */
	sbi(STATE_LED_DDR, STATE_LED);

	/* Настриваем на ВЫХОД порт для меандра  */
	sbi(MEANDER_DDR, MEANDER); 
}


// настройка прерывания от внешнего источник (кнопки)
void initExtInt0(){
	// срабатывание по низкому уровню на выводе INT0
	cbi(MCUCR, ISC00);
	cbi(MCUCR, ISC01);
	// резрещаем внешние прерывания
	sbi(GICR, INT0);
}

// настройка ТС2 - генерации меандра 
// TC2 настроен на режим СТС (сброс при сравнении).
void initTC2(){
	// разрешаем прерывания от этого таймера
	sbi(TIMSK, OCIE2);
	// задаем режим СТС. Atmega datasheet p.115 table 42.
	sbi(TCCR2, WGM21);
	// выставляем максимальеый делитель = 1024. Atmega datasheet p.116 table 46.
	sbi(TCCR2, CS20);
	sbi(TCCR2, CS21);
	sbi(TCCR2, CS22);
	// От этого значения зависит частота срабатывания. Расчет выполняеться по формуле:
	// f[OCn]= f[clk-io]/(2*N*(1+OCR2))
	// где N - делитель;
	// Причем эту полученную частоту нужно разделить пополам: f[OC2]=f[clk-io]
	// Более детально Atmega datasheet p.109.
	// Соответственно для ocr2=195 -> t=25 ms.
	OCR2 = 195;
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
	while (iscbi(OPTO_SENSOR_PIN, OPTO_SENSOR)){
		SM_PORT = smTableNormalStep[3];
		_delay_ms(SM_DELAY_STEP_MS);
	
		SM_PORT = smTableNormalStep[2];
		_delay_ms(SM_DELAY_STEP_MS);
		
		SM_PORT = smTableNormalStep[1];
		_delay_ms(SM_DELAY_STEP_MS);
		
		SM_PORT = smTableNormalStep[0];
		_delay_ms(SM_DELAY_STEP_MS);
	}
	stopSM();
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

void blinkLed1r(){
	sbi(STATE_LED_PORT, STATE_LED);
	_delay_ms(100);
	cbi(STATE_LED_PORT, STATE_LED);
}

void blinkLed2r(){
	sbi(STATE_LED_PORT, STATE_LED);
	_delay_ms(30);
	cbi(STATE_LED_PORT, STATE_LED);
	_delay_ms(30);
	sbi(STATE_LED_PORT, STATE_LED);
	_delay_ms(30);
	cbi(STATE_LED_PORT, STATE_LED);
}
