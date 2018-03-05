package com.zanktech.autotrading.forecasting.strategy;

import java.util.ArrayList;
import java.util.List;

import com.dukascopy.api.IBar;
import com.dukascopy.api.IEngine.OrderCommand;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.Period;
import com.zanktech.autotrading.forecasting.AbstractSignalProvider;
import com.zanktech.autotrading.forecasting.ForecastUtils;
import com.zanktech.autotrading.model.impl.OrderRecord;

public class candlePattern extends AbstractSignalProvider{
	/**
	 * Creater: ASUS
	 * Creation Date: 2015年11月18日
	 * Description:
	 *
	 */
	private final Period update= Period.FOUR_HOURS;
	private ArrayList<IBar> bars;

	//A rare reversal pattern characterized by a gap followed by a Doji,
	//which is then followed by another gap in the opposite direction.
	//The shadows on the Doji must completely gap below or above the shadows of the first and third day.
	// 1: buy 2: sell 0: do nothing
	private int abandonedBaby()
	{
		int s = this.bars.size();
		if(s>3)
		{
			//System.out.println("in testing: "+ this.bars.size());
			// s-3 阴， s-2 十字， s-1 阳
			if(((this.bars.get(s-3).getOpen()-this.bars.get(s-3).getClose())>0.0002)
					&&
					((this.bars.get(s-3).getLow()-this.bars.get(s-2).getHigh())>0.0001)
					&&
					((this.bars.get(s-1).getLow()-this.bars.get(s-2).getHigh())>0.0001)
					&&
					((this.bars.get(s-1).getClose()-this.bars.get(s-1).getOpen())>0.0002)
					) {
				return 2;
			} else if (
					((this.bars.get(s-3).getClose()-this.bars.get(s-3).getOpen())>0.0002)
					&&
					((this.bars.get(s-2).getLow()-this.bars.get(s-3).getHigh())>0.0001)
					&&
					((this.bars.get(s-2).getLow()-this.bars.get(s-1).getHigh())>0.0001)
					&&
					((this.bars.get(s-1).getOpen()-this.bars.get(s-1).getClose())>0.0002)
					) {
				return 1;
			} else {
				return 0;
			}
		} else {
			return 0;
		}
	}
	// A bearish reversal pattern that continues the uptrend with a long white body.
	//The next day opens at a new high then closes below the midpoint of the body of the first day.
	// 1:buy, 2:sell, 0: do nothing
	// NOT USEFUL
	private int darkCloudCover()
	{
		//	System.out.println("in testing: "+ this.bars.size());
		int s= this.bars.size();
		if(s>2)
		{

			if(
					(this.bars.get(s-2).getClose()>this.bars.get(s-2).getOpen())
					&&
					((this.bars.get(s-1).getClose()-((this.bars.get(s-2).getClose()+this.bars.get(s-1).getOpen())/2))<-0.0005)
					&&((this.bars.get(s-1).getOpen()-this.bars.get(s-2).getClose())>0.0001
							)
					) {
				System.out.println("buy first bar side: "+ (this.bars.get(s-2).getClose()-this.bars.get(s-2).getOpen()));
				System.out.println("two bars gaps: "+ (this.bars.get(s-1).getClose()-((this.bars.get(s-2).getClose()+this.bars.get(s-1).getOpen())/2)) );
				return 2;
			} else if(
					((this.bars.get(s-2).getOpen()>this.bars.get(s-2).getClose()))
					&&
					((this.bars.get(s-1).getClose()-((this.bars.get(s-2).getClose()+this.bars.get(s-1).getOpen())/2))>0.0002)
					&&
					((this.bars.get(s-2).getClose()-this.bars.get(s-1).getOpen())>0.0001)
					) {
				System.out.println("sell first bar side: "+ (this.bars.get(s-2).getOpen()-this.bars.get(s-2).getClose()));
				System.out.println("two bars gaps: " + (this.bars.get(s-1).getClose()-((this.bars.get(s-2).getClose()+this.bars.get(s-1).getOpen())/2)));
				return 1;
			} else {
				return 0;
			}
		} else {
			return 0;
		}
	}
	// means buy and sell are equal strength. Need to pay attention for the next bar
	// maybe close the order when doji happens
	private boolean doji()
	{
		int s = this.bars.size();
		if((this.bars.get(s-1).getOpen()==this.bars.get(s-1).getClose())
				&& (this.bars.get(s-1).getHigh()!=this.bars.get(s-1).getClose())
				&&(this.bars.get(s-1).getLow()!=this.bars.get(s-1).getClose())
				) {
			return true;
		} else {
			return false;
		}
	}
	//A Doji where the open and close price are at the high of the day.
	//Like other Doji days, this one normally appears at market turning points.
	private int dragonflyDoji()
	{
		int s =this.bars.size();
		if((this.bars.get(s-1).getOpen()==this.bars.get(s-1).getClose())  &&
				(this.bars.get(s-1).getHigh()==this.bars.get(s-1).getClose()) ) {
			return 2;
		} else if((this.bars.get(s-1).getOpen()==this.bars.get(s-1).getClose()) && (this.bars.get(s-1).getLow()==this.bars.get(s-1).getClose()) ) {
			return 1;
		} else {
			return 0;
		}
	}
	// A reversal pattern that can be bearish or bullish, depending upon whether it appears at the end of an uptrend (bearish engulfing pattern) or a downtrend (bullish engulfing pattern).
	//The first day is characterized by a small body, followed by a day whose body completely engulfs the previous day's body.
	private int engulfingPattern()
	{
		int s = this.bars.size();
		if(s>3)
		{
			if((this.bars.get(s-4).getClose()>this.bars.get(s-3).getClose())
					&&
					((this.bars.get(s-2).getOpen()-this.bars.get(s-1).getClose())<-0.0010)
					&&
					(this.bars.get(s-3).getClose()>this.bars.get(s-2).getClose())
					) {
				return 1;
			} else if(
					(this.bars.get(s-4).getClose()<this.bars.get(s-3).getClose())
					&&
					(this.bars.get(s-3).getClose()<this.bars.get(s-2).getClose())
					&&
					((this.bars.get(s-2).getClose()-this.bars.get(s-1).getOpen())<-0.0010)
					) {
				return 2;
			} else {
				return 0;
			}
		} else {
			return 0;
		}
	}
	private int eveningStar()
	{
		int s = this.bars.size();
		if(s>3)
		{
			if(((this.bars.get(s-2).getLow()-this.bars.get(s-3).getHigh())>0.0002)
					&&
					((this.bars.get(s-1).getOpen()-this.bars.get(s-1).getClose())>0.0010)
					&&
					(this.bars.get(s-1).getClose()<((this.bars.get(s-3).getLow()+this.bars.get(s-3).getHigh())/2))) {
				return 2;
			} else if(
					((this.bars.get(s-3).getLow()-this.bars.get(s-2).getHigh())>0.0002)
					&&
					((this.bars.get(s-1).getClose()-this.bars.get(s-1).getOpen())>0.0010)
					&&
					(this.bars.get(s-1).getClose()>((this.bars.get(s-3).getLow()+this.bars.get(s-3).getHigh())/2))
					) {
				return 1;
			} else {
				return 0;
			}

		} else {
			return 0;
		}
	}
	//statistic consideration for each pattern
	@Override
	public OrderRecord initTrade(Instrument instrument, Period period,
			IBar askBar, IBar bidBar) throws JFException {
		// TODO Auto-generated method stub
		OrderRecord initOrder = null;
		if((period == this.update) && (askBar.getVolume()!=0.0) )
		{
			this.bars.add(askBar);
			if(this.bars.size()>4) {
				this.bars.remove(0);
			}
			if(this.engulfingPattern()==1)
			{

				initOrder = ForecastUtils.generateOrderSignal("candle",instrument,
						OrderCommand.BUY,askBar.getClose()-0.0016,askBar.getClose()+0.0035,"buy", 0.0);
			}
			else if(this.engulfingPattern()==2)
			{
				initOrder = ForecastUtils.generateOrderSignal("candle",instrument,
						OrderCommand.SELL,askBar.getClose()+0.0016,askBar.getClose()-0.0035,"sell", 0.0);
			}
		}
		return initOrder;
	}
	private int invertedHammer()
	{
		int s = this.bars.size();
		if(s>2)
		{
			if(((this.bars.get(s-2).getOpen()-this.bars.get(s-2).getClose())>0.0015)
					&&
					((this.bars.get(s-2).getClose()-this.bars.get(s-1).getClose())<0.0001)) {
				return 1;
			} else if(((this.bars.get(s-2).getClose()-this.bars.get(s-2).getOpen())>0.0015)
					&&
					((this.bars.get(s-1).getClose()-this.bars.get(s-2).getClose())<0.0001)) {
				return 2;
			} else {
				return 0;
			}

		} else {
			return 0;
		}
	}
	//	@Override
	@Override
	protected void setParameter(Object param) {
		// TODO Auto-generated method stub
		this.bars = new ArrayList<IBar>();
	}

	// a bullish reversal pattern
	private int stickSandwich()
	{
		int s = this.bars.size();
		if(s>3)
		{
			if((this.bars.get(s-3).getClose()==this.bars.get(s-1).getClose())
					&& (this.bars.get(s-1).getOpen()>this.bars.get(s-2).getClose())
					&& (this.bars.get(s-1).getOpen()>this.bars.get(s-1).getClose())
					&& (this.bars.get(s-3).getOpen()>this.bars.get(s-3).getClose())
					) {
				return 1;
			} else {
				return 0;
			}

		} else {
			return 0;
		}
	}
	@Override
	public void updateTrade(Instrument instrument, Period period, IBar askBar,
			IBar bidBar, List<IOrder> set) throws JFException {
		// TODO Auto-generated method stub
		if((period == this.update) && (askBar.getVolume()!=0.0))
		{
			int aBaby = this.engulfingPattern();
			//	System.out.println(aBaby);
			if(aBaby==1)
			{
				for(IOrder o: set)
				{
					//					if(o.getOrderCommand().equals(OrderCommand.BUY))
					//					{
					//						if(askBar.getClose()>(o.getOpenPrice()+0.0005)) {
					//							o.setStopLossPrice(askBar.getClose()-0.0020);
					//						}
					//					}
					if(o.getOrderCommand().equals(OrderCommand.SELL)) {
						o.close();
					}
				}
			}
			else if(aBaby==0)
			{

				for(IOrder o: set)
				{
					//					if(o.getOrderCommand().equals(OrderCommand.SELL))
					//					{
					//						if(askBar.getClose()<(o.getOpenPrice()-0.0005)) {
					//							o.setStopLossPrice(askBar.getClose());
					//						}
					//					}
					if(o.getOrderCommand().equals(OrderCommand.BUY)) {
						o.close();
					}
				}
			}
		}

	}
}
