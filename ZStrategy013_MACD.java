

package com.zanktech.autotrading.forecasting.strategy;

import java.util.List;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import com.dukascopy.api.IBar;
import com.dukascopy.api.IEngine.OrderCommand;
import com.dukascopy.api.IIndicators.AppliedPrice;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.zanktech.autotrading.forecasting.AbstractSignalProvider;
import com.zanktech.autotrading.forecasting.ForecastUtils;
import com.zanktech.autotrading.model.impl.OrderRecord;
import com.zanktech.autotrading.utils.FixedSizeQueue;

public class ZStrategy013_MACD extends AbstractSignalProvider {

	private Period tradingPeriod;
	private FixedSizeQueue<Double> maQueue;
	private boolean isInitial;
	private double fixedStoploss;
	private FixedSizeQueue<Integer> macdHistStatus;
	private final int zeroNumberTol=4;
	private int initialCount=15;
	private int waitingStatus;
	private int waitingCount;



	public ZStrategy013_MACD() {
		// TODO Auto-generated constructor stub
	}
	private boolean downCross()
	{
		if(this.macdHistStatus.get(14)==2)
		{
			for(int i =1;i<=4;i++)
			{
				if(this.macdHistStatus.get(14-i)==2)
				{
					return false;
				}
				else if(this.macdHistStatus.get(14-i)==1)
				{
					return true;
				}
			}
		}
		return false;
	}


	@Override
	public OrderRecord initTrade(Instrument instrument, Period period,
			IBar askBar, IBar bidBar) throws JFException {
		// TODO Auto-generated method stub
		OrderRecord o = null;

		if(period.equals(this.tradingPeriod)&&(this.initialCount==0))
		{
			if(this.waitingStatus==1)
			{
				if((this.macdHistStatus.peekLast()==2)||(this.macdHistStatus.peekLast()==0))
				{
					this.waitingStatus=0;
					this.waitingCount=0;
				}

				if(this.macdHistStatus.peekLast()==1)
				{
					this.waitingCount++;
				}
			}

			if(this.waitingStatus==2)
			{
				if((this.macdHistStatus.peekLast()==1)||(this.macdHistStatus.peekLast()==0))
				{
					this.waitingStatus=0;
					this.waitingCount=0;
				}
				if(this.macdHistStatus.peekLast()==2)
				{
					this.waitingCount++;
				}
			}

			if((this.trendType()==1)&&(this.upCross())&&(askBar.getClose()>this.maQueue.peekLast()))
			{
				System.out.println("type:"+this.trendType());
				this.waitingStatus=1;

			}

			if((this.trendType()==2)&&(this.downCross())&&(askBar.getClose()<this.maQueue.peekLast()))
			{
				System.out.println("type:"+this.trendType());
				this.waitingStatus=2;

			}
			if((this.waitingStatus==1)&&(this.waitingCount==2))
			{
				o=ForecastUtils.generateOrderSignal("macd", instrument, OrderCommand.BUY,
						askBar.getClose()-this.fixedStoploss, 0, "buy", 0);
				this.waitingCount=0;
				this.waitingStatus=0;
			}

			if((this.waitingStatus==2)&&(this.waitingCount==2))
			{
				o=ForecastUtils.generateOrderSignal("macd", instrument, OrderCommand.SELL,
						askBar.getClose()+this.fixedStoploss, 0, "sell", 0);
				this.waitingCount=0;
				this.waitingStatus=0;
			}


		}
		return o;
	}

	@Override
	protected void setParameter(Object param) {
		// TODO Auto-generated method stub
		this.maQueue= new FixedSizeQueue<Double>(15);
		this.macdHistStatus = new FixedSizeQueue<Integer>(15);
		this.isInitial= true;
		this.tradingPeriod= Period.FOUR_HOURS;
		this.fixedStoploss= 0.015;
		this.waitingCount=0;
		this.waitingStatus=0;


	}

	private int trendType()
	{
		SimpleRegression reg = new SimpleRegression();
		double regCount=0;
		for(Double ma:this.maQueue)
		{
			reg.addData(regCount,ma);
			regCount=regCount+0.001;
		}
		System.out.println("ma slope"+ reg.getSlope());
		if(reg.getSlope()<-0.2)
		{
			return 2;
		}
		else if(reg.getSlope()>0.2)
		{
			return 1;
		}
		else
		{
			return 0;
		}
	}

	private boolean upCross()
	{
		if(this.macdHistStatus.get(14)==1)
		{
			for(int i =1;i<=4;i++)
			{
				if(this.macdHistStatus.get(14-i)==1)
				{
					return false;
				}
				else if(this.macdHistStatus.get(14-i)==2)
				{
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void updateTrade(Instrument instrument, Period period, IBar askBar,
			IBar bidBar, List<IOrder> set) throws JFException {
		// TODO Auto-generated method stub
		if (period.equals(this.tradingPeriod)) {
			//			if(this.isInitial)
			//			{
			//				for(int i=15;i>1;i++)
			//				{
			//					this.maQueue.add(this.indicators.sma(instrument, Period.FOUR_HOURS, OfferSide.ASK, AppliedPrice.CLOSE, 120,i));
			//					int thisHistStatus;
			//					double[] macdValues = this.indicators.macd(instrument, period, OfferSide.ASK, AppliedPrice.CLOSE, 12, 26, 9, i);
			//					if(macdValues[2]>0.0001)
			//					{
			//						this.macdHistStatus.add(1);
			//					}
			//					else if(macdValues[2]<0.0001)
			//					{
			//						this.macdHistStatus.add(2);
			//
			//					}
			//					else
			//					{
			//						this.macdHistStatus.add(0);
			//					}
			//
			//				}
			//				this.isInitial=false;
			//			}
			double[] macdValues = this.indicators.macd(instrument, period, OfferSide.ASK, AppliedPrice.CLOSE, 12, 30, 9, 1);
			double maValue = this.indicators.ema(instrument, period, OfferSide.ASK, AppliedPrice.CLOSE, 55, 1);
			this.maQueue.add(maValue);
			int thisHistStatus;
			if(macdValues[2]>0.0002)
			{
				this.macdHistStatus.add(1);
			}
			else if(macdValues[2]<-0.0002)
			{
				this.macdHistStatus.add(2);

			}
			else
			{
				this.macdHistStatus.add(0);
			}
			System.out.println("hist value"+macdValues[2]);
			if(this.initialCount>0)
			{
				this.initialCount--;
			}
			for (IOrder o : set) {
				if(o.getOrderCommand().equals(OrderCommand.BUY))
				{
					if(this.downCross())
					{
						o.close();
					}
				}

				if(o.getOrderCommand().equals(OrderCommand.SELL))
				{
					if(this.upCross())
					{
						o.close();
					}
				}
			}
		}

	}
}
