export class StereoWidener {
  private readonly context: AudioContext
  public readonly inputNode: GainNode
  public readonly outputNode: GainNode

  private readonly splitter: ChannelSplitterNode
  private readonly merger: ChannelMergerNode

  private readonly midNode: GainNode
  private readonly sideNode: GainNode
  private readonly sideWidthGain: GainNode
  private readonly headroomGain: GainNode

  private readonly leftSumNode: GainNode
  private readonly rightSumNode: GainNode
  private readonly rightInverterNode: GainNode

  private readonly gainLToMid: GainNode
  private readonly gainRToMid: GainNode
  private readonly gainLToSide: GainNode
  private readonly gainRToSide: GainNode

  private currentWidth = 1.0

  constructor(context: AudioContext) {
    this.context = context
    const now = this.context.currentTime

    this.inputNode = context.createGain()
    this.outputNode = context.createGain()

    this.splitter = context.createChannelSplitter(2)
    this.merger = context.createChannelMerger(2)

    this.midNode = context.createGain()
    this.sideNode = context.createGain()
    this.sideWidthGain = context.createGain()
    this.headroomGain = context.createGain()

    this.sideWidthGain.gain.setValueAtTime(this.currentWidth, now)
    this.headroomGain.gain.setValueAtTime(1.0, now)

    this.gainLToMid = context.createGain()
    this.gainLToMid.gain.setValueAtTime(0.5, now)
    this.gainRToMid = context.createGain()
    this.gainRToMid.gain.setValueAtTime(0.5, now)

    this.gainLToSide = context.createGain()
    this.gainLToSide.gain.setValueAtTime(0.5, now)
    this.gainRToSide = context.createGain()
    this.gainRToSide.gain.setValueAtTime(-0.5, now)

    this.leftSumNode = context.createGain()
    this.rightSumNode = context.createGain()
    this.rightInverterNode = context.createGain()
    this.rightInverterNode.gain.setValueAtTime(-1.0, now)

    this.inputNode.connect(this.splitter)

    this.splitter.connect(this.gainLToMid, 0)
    this.splitter.connect(this.gainRToMid, 1)
    this.gainLToMid.connect(this.midNode)
    this.gainRToMid.connect(this.midNode)

    this.splitter.connect(this.gainLToSide, 0)
    this.splitter.connect(this.gainRToSide, 1)
    this.gainLToSide.connect(this.sideNode)
    this.gainRToSide.connect(this.sideNode)

    this.sideNode.connect(this.sideWidthGain)

    this.midNode.connect(this.leftSumNode)
    this.sideWidthGain.connect(this.leftSumNode)

    this.midNode.connect(this.rightSumNode)
    this.sideWidthGain.connect(this.rightInverterNode)
    this.rightInverterNode.connect(this.rightSumNode)

    this.leftSumNode.connect(this.merger, 0, 0)
    this.rightSumNode.connect(this.merger, 0, 1)

    this.merger.connect(this.headroomGain)
    this.headroomGain.connect(this.outputNode)
  }

  public setWidth(width: number): void {
    const clamped = Math.max(0.0, Math.min(2.0, width))
    this.currentWidth = clamped
    const now = this.context.currentTime

    this.sideWidthGain.gain.cancelScheduledValues(now)
    this.sideWidthGain.gain.setTargetAtTime(clamped, now, 0.05)

    // At high width (1.0 < w <= 2.0), side energy can exceed 0 dBFS.
    // Compensate headroom to prevent digital clipping above full scale.
    const headroomFactor = clamped > 1.0 ? 1.0 / Math.sqrt(1.0 + 0.5 * (clamped - 1.0)) : 1.0
    this.headroomGain.gain.cancelScheduledValues(now)
    this.headroomGain.gain.setTargetAtTime(headroomFactor, now, 0.05)
  }

  public getWidth(): number {
    return this.currentWidth
  }

  public cleanup(): void {
    const now = this.context.currentTime
    this.sideWidthGain.gain.cancelScheduledValues(now)
    this.headroomGain.gain.cancelScheduledValues(now)

    this.inputNode.disconnect()
    this.splitter.disconnect()
    this.gainLToMid.disconnect()
    this.gainRToMid.disconnect()
    this.gainLToSide.disconnect()
    this.gainRToSide.disconnect()
    this.midNode.disconnect()
    this.sideNode.disconnect()
    this.sideWidthGain.disconnect()
    this.leftSumNode.disconnect()
    this.rightInverterNode.disconnect()
    this.rightSumNode.disconnect()
    this.merger.disconnect()
    this.headroomGain.disconnect()
    this.outputNode.disconnect()
  }
}
