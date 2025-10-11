#--
# Copyright (c) 2010-2012 Engine Yard, Inc.
# Copyright (c) 2007-2009 Sun Microsystems, Inc.
# This source code is available under the MIT license.
# See the file LICENSE.txt for details.
#++

require File.expand_path('spec_helper', File.dirname(__FILE__) + '/..')
require 'jruby/rack'

describe JRuby::Rack::Capture do

  before :each do
    JRuby::Rack.context = nil
    $servlet_context = @servlet_context
    allow(@servlet_context).to receive(:init_parameter_names).and_return []
    allow(@servlet_context).to receive(:config).and_return Java::OrgJrubyRackEmbed::Config.new
  end

  it "captures environment information" do
    expect(@servlet_context).to receive(:log)
    error = StandardError.new
    error.capture
    error.store
    expect(error.output).to be_a StringIO
    expect(error.output.string).to include "blah"
  end

  it "captures exception backtrace" do
    begin
      raise ZeroDivisionError.new
    rescue ZeroDivisionError => e
      e.capture
      expect(e.output.string).to match /--- Backtrace/
      expect(e.output.string).to match /ZeroDivisionError/
    end
  end

  it "handles jruby 10 compatibility" do
    expect(@servlet_context).to receive(:log)
    error = Exception.new "rack start-up failed"
    error.capture
    error.store
    expect(error.output).to be_a StringIO
    expect(error.output.string).to include "blah"
  end

end
