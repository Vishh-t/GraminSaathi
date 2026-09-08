import { Component } from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';

export default class AppErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null, info: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, info) {
    // Surface the real error in the browser console with full stack + component stack
    console.error('GraminSaathi crashed:', error, info?.componentStack);
    this.setState({ info });
  }

  handleReload = () => {
    window.location.href = '/dashboard';
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen bg-[#f9fafb] flex items-center justify-center p-6">
          <div className="max-w-xl w-full bg-white border border-red-200 rounded-lg shadow-sm p-6">
            <div className="flex items-start gap-3 mb-4">
              <div className="w-10 h-10 rounded-full bg-red-100 flex items-center justify-center flex-shrink-0">
                <AlertTriangle className="w-5 h-5 text-red-600" />
              </div>
              <div>
                <h2 className="text-lg font-semibold text-gray-900">Something broke on this page</h2>
                <p className="text-sm text-gray-500 mt-0.5">
                  This is the real error &mdash; copy it and share it so it can be fixed.
                </p>
              </div>
            </div>
            <pre className="bg-gray-900 text-red-300 text-xs rounded-lg p-4 overflow-auto max-h-64 whitespace-pre-wrap">
              {this.state.error?.toString()}
              {this.state.info?.componentStack ? `\n\nComponent stack:${this.state.info.componentStack}` : ''}
            </pre>
            <button onClick={this.handleReload} className="btn-primary mt-4 flex items-center gap-2">
              <RefreshCw className="w-4 h-4" />
              Back to Dashboard
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
